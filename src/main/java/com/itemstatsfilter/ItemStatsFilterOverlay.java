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

/**
 * Removes the Item Stats plugin's tooltips from the frame before they are drawn.
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

		if (tooltips.size() > before)
		{
			final List<Tooltip> itemStatsTooltips = new ArrayList<>(tooltips.subList(before, tooltips.size()));
			// Removes the probe's copies and the Item Stats plugin's originals
			tooltips.removeAll(itemStatsTooltips);
		}

		return null;
	}
}
