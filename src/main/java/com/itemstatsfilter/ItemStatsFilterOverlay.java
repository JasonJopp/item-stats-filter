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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.plugins.itemstats.ItemStatOverlay;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;

/**
 * Removes the Item Stats plugin's tooltips from the frame before they are drawn, and
 * marks the item when it does.
 * <p>
 * Each frame, overlays queue tooltips in the TooltipManager and RuneLite's tooltip
 * overlay draws and clears them afterwards. Item Stats queues its tooltips under the
 * game interfaces; this overlay runs above them, after Item Stats but before the
 * tooltip overlay (ABOVE_WIDGETS, and tooltip-positioned overlays sort last).
 * <p>
 * To tell Item Stats' tooltips apart from other plugins', it runs a private copy of the
 * Item Stats overlay with the same settings. That copy appends exactly the tooltips
 * Item Stats added this frame, and every matching tooltip is then removed.
 */
class ItemStatsFilterOverlay extends Overlay
{
	private static final String MARKER = ColorUtil.wrapWithColorTag("...", Color.LIGHT_GRAY);
	private static final String ASTERISK = "*";

	private final ItemStatsFilterPlugin plugin;
	private final TooltipManager tooltipManager;
	private final ItemStatOverlay itemStatsProbe;

	ItemStatsFilterOverlay(ItemStatsFilterPlugin plugin, TooltipManager tooltipManager, ItemStatOverlay itemStatsProbe)
	{
		super(plugin);
		this.plugin = plugin;
		this.tooltipManager = tooltipManager;
		this.itemStatsProbe = itemStatsProbe;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		// Mouse Tooltips shares this layer and position at the default priority. Dynamic
		// overlays with a higher priority draw later, so this runs after its box is queued.
		setPriority(PRIORITY_HIGHEST);
		// The tooltip overlay also draws after this interface (fullscreen world map, welcome screen)
		drawAfterInterface(InterfaceID.TOPLEVEL_DISPLAY);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.shouldHideItemStats())
		{
			return null;
		}

		final List<Tooltip> tooltips = tooltipManager.getTooltips();
		final int before = tooltips.size();
		itemStatsProbe.render(graphics);

		if (tooltips.size() == before)
		{
			return null;
		}

		final List<Tooltip> itemStatsTooltips = new ArrayList<>(tooltips.subList(before, tooltips.size()));

		// Where the Item Stats plugin's own tooltips start. -1 when it showed none this
		// frame (plugin off, or its tooltips disabled), so there is nothing to mark.
		int shownAt = -1;
		for (int i = 0; i < before; i++)
		{
			if (itemStatsTooltips.contains(tooltips.get(i)))
			{
				shownAt = i;
				break;
			}
		}

		// Removes the probe's copies and the Item Stats plugin's originals
		tooltips.removeAll(itemStatsTooltips);

		if (shownAt >= 0)
		{
			markHidden(tooltips, shownAt);
		}

		return null;
	}

	private void markHidden(List<Tooltip> tooltips, int index)
	{
		switch (plugin.getIndicator())
		{
			case ASTERISK:
			{
				// Only shows when the Mouse Tooltips plugin has queued its box
				final String mouseText = plugin.getMouseTooltipText();
				if (mouseText == null)
				{
					return;
				}

				for (Tooltip tooltip : tooltips)
				{
					if (mouseText.equals(tooltip.getText()))
					{
						tooltip.setText(mouseText + ASTERISK);
						return;
					}
				}
				break;
			}
			case MARKER_BOX:
				// Where the stats box would have been
				tooltips.add(Math.min(index, tooltips.size()), new Tooltip(MARKER));
				break;
			case NONE:
				break;
		}
	}
}
