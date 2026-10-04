/*
 * Copyright (c) 2026, Creameo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.itemstatsfilter;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.itemstats.ItemStatConfig;
import net.runelite.client.plugins.itemstats.ItemStatOverlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

@PluginDescriptor(
	name = "Item Stats Filter",
	description = "Hide Item Stats tooltips for chosen items, or only show them while a key is held",
	tags = {"item", "stats", "tooltip", "hide", "blacklist", "hotkey", "equipment"}
)
public class ItemStatsFilterPlugin extends Plugin
{
	private static final String HIDE_OPTION = "Hide stats";
	private static final String SHOW_OPTION = "Show stats";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TooltipManager tooltipManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ItemStatsFilterConfig config;

	private ItemStatsFilterOverlay overlay;

	// While running, these are only used on the client thread (overlay rendering, menu
	// events, and config changes routed through clientThread.invoke).
	private volatile List<String> hiddenPatterns = Collections.emptyList();
	private final Map<Integer, Boolean> hiddenCache = new HashMap<>();

	private HotkeyListener hotkeyListener;
	private volatile boolean hotkeyHeld;

	@Provides
	ItemStatsFilterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ItemStatsFilterConfig.class);
	}

	@Override
	protected void startUp()
	{
		if (overlay == null)
		{
			overlay = new ItemStatsFilterOverlay(this, tooltipManager, createItemStatsProbe());
		}

		updateHiddenPatterns();
		updateHotkeyListener();
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		if (hotkeyListener != null)
		{
			keyManager.unregisterKeyListener(hotkeyListener);
			hotkeyListener = null;
		}
		hotkeyHeld = false;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!ItemStatsFilterConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		switch (event.getKey())
		{
			case ItemStatsFilterConfig.HIDDEN_ITEMS:
				clientThread.invoke(this::updateHiddenPatterns);
				break;
			case ItemStatsFilterConfig.REQUIRE_HOTKEY:
			case ItemStatsFilterConfig.HOTKEY:
				updateHotkeyListener();
				break;
		}
	}

	/**
	 * Shift + right-click on an item adds "Hide stats" / "Show stats" just below Examine.
	 */
	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (!config.shiftClickToggle() || !client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}

		final MenuEntry[] entries = event.getMenuEntries();
		for (int idx = entries.length - 1; idx >= 0; --idx)
		{
			final MenuEntry entry = entries[idx];
			final Widget widget = entry.getWidget();
			if (widget == null || !"Examine".equals(entry.getOption()) || !isStatTooltipWidget(widget))
			{
				continue;
			}

			final int itemId = getItemId(widget);
			if (itemId <= 0)
			{
				return;
			}

			final String name = itemManager.getItemComposition(itemId).getName();
			final boolean listedByName = isListedByName(name);
			if (!listedByName && isHidden(itemId))
			{
				// Hidden by a wildcard entry; that has to be edited in the config panel.
				return;
			}

			client.getMenu().createMenuEntry(idx)
				.setOption(listedByName ? SHOW_OPTION : HIDE_OPTION)
				.setTarget(entry.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(e -> toggleHidden(name));
			return;
		}
	}

	/**
	 * Whether the Item Stats tooltips should be removed this frame. Client thread only.
	 */
	boolean shouldHideItemStats()
	{
		if (config.requireHotkey() && !hotkeyHeld)
		{
			return true;
		}

		if (hiddenPatterns.isEmpty())
		{
			return false;
		}

		final int itemId = getHoveredItemId();
		return itemId > 0 && isHidden(itemId);
	}

	/**
	 * A private copy of RuneLite's Item Stats tooltip overlay, reading the Item Stats
	 * plugin's own settings. It is never drawn; {@link ItemStatsFilterOverlay} runs it to
	 * learn exactly which tooltips Item Stats added this frame, so it can remove them.
	 * <p>
	 * The ItemStatConfig binding lives in a private child injector on purpose. RuneLite
	 * builds a plugin's settings panel from the first Config type bound in the plugin's
	 * own injector, so binding it there would replace this plugin's panel with Item Stats'.
	 */
	private ItemStatOverlay createItemStatsProbe()
	{
		final ItemStatConfig itemStatConfig = configManager.getConfig(ItemStatConfig.class);
		return getInjector()
			.createChildInjector(binder -> binder.bind(ItemStatConfig.class).toInstance(itemStatConfig))
			.getInstance(ItemStatOverlay.class);
	}

	/**
	 * Whether this item's name matches the hidden list. Client thread only.
	 */
	boolean isHidden(int itemId)
	{
		if (hiddenPatterns.isEmpty())
		{
			return false;
		}

		return hiddenCache.computeIfAbsent(itemId, id ->
		{
			final String name = itemManager.getItemComposition(id).getName();
			for (String pattern : hiddenPatterns)
			{
				if (WildcardMatcher.matches(pattern, name))
				{
					return true;
				}
			}
			return false;
		});
	}

	/**
	 * The item under the mouse, found the same way the Item Stats overlay finds it.
	 */
	private int getHoveredItemId()
	{
		final MenuEntry[] menu = client.getMenu().getMenuEntries();
		if (menu.length == 0)
		{
			return -1;
		}

		final Widget widget = menu[menu.length - 1].getWidget();
		return widget != null ? getItemId(widget) : -1;
	}

	private static int getItemId(Widget widget)
	{
		final int group = WidgetUtil.componentToInterface(widget.getId());

		// Worn equipment slots keep the item on a child widget
		if (group == InterfaceID.WORNITEMS
			|| (group == InterfaceID.BANKMAIN && widget.getParentId() == InterfaceID.Bankside.WORNOPS))
		{
			final Widget item = widget.getChild(1);
			return item != null ? item.getItemId() : -1;
		}

		return widget.getItemId();
	}

	/**
	 * Places where the Item Stats plugin can show a tooltip.
	 */
	private static boolean isStatTooltipWidget(Widget widget)
	{
		final int group = WidgetUtil.componentToInterface(widget.getId());
		return widget.getId() == InterfaceID.Inventory.ITEMS
			|| group == InterfaceID.WORNITEMS
			|| group == InterfaceID.EQUIPMENT_SIDE
			|| group == InterfaceID.BANKMAIN
			|| group == InterfaceID.BANKSIDE
			|| widget.getId() == InterfaceID.SharedBank.ITEMS
			|| group == InterfaceID.SHARED_BANK_SIDE;
	}

	private boolean isListedByName(String name)
	{
		for (String entry : Text.fromCSV(config.hiddenItems()))
		{
			if (entry.equalsIgnoreCase(name))
			{
				return true;
			}
		}
		return false;
	}

	private void toggleHidden(String name)
	{
		final List<String> items = new ArrayList<>(Text.fromCSV(config.hiddenItems()));
		if (!items.removeIf(name::equalsIgnoreCase))
		{
			items.add(name);
		}
		configManager.setConfiguration(ItemStatsFilterConfig.GROUP, ItemStatsFilterConfig.HIDDEN_ITEMS, Text.toCSV(items));
	}

	private void updateHiddenPatterns()
	{
		hiddenPatterns = Text.fromCSV(config.hiddenItems());
		hiddenCache.clear();
	}

	private void updateHotkeyListener()
	{
		if (hotkeyListener != null)
		{
			keyManager.unregisterKeyListener(hotkeyListener);
			hotkeyListener = null;
		}
		hotkeyHeld = false;

		// Only listen while the feature is on, so a bound key isn't swallowed otherwise
		if (config.requireHotkey())
		{
			hotkeyListener = new HotkeyListener(config::hotkey)
			{
				@Override
				public void hotkeyPressed()
				{
					hotkeyHeld = true;
				}

				@Override
				public void hotkeyReleased()
				{
					hotkeyHeld = false;
				}
			};
			keyManager.registerKeyListener(hotkeyListener);
		}
	}
}
