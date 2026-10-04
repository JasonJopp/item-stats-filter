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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

@ConfigGroup(ItemStatsFilterConfig.GROUP)
public interface ItemStatsFilterConfig extends Config
{
	String GROUP = "itemstatsfilter";
	String HIDDEN_ITEMS = "hiddenItems";
	String REQUIRE_HOTKEY = "requireHotkey";
	String HOTKEY = "hotkey";

	@ConfigSection(
		name = "Hold to show",
		description = "Only show stat tooltips while a key is held",
		position = 10
	)
	String hotkeySection = "hotkeySection";

	@ConfigItem(
		keyName = HIDDEN_ITEMS,
		name = "Hidden items",
		description = "Items that never show a stat tooltip. Comma-separated names, * is a wildcard (e.g. Prayer potion*)",
		position = 0
	)
	default String hiddenItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "shiftClickToggle",
		name = "Shift-click option",
		description = "Shift + right-click an item to get a 'Hide stats' / 'Show stats' option that edits the hidden list",
		position = 1
	)
	default boolean shiftClickToggle()
	{
		return true;
	}

	@ConfigItem(
		keyName = REQUIRE_HOTKEY,
		name = "Only show while held",
		description = "Only show stat tooltips while the key below is held down",
		position = 11,
		section = hotkeySection
	)
	default boolean requireHotkey()
	{
		return false;
	}

	@ConfigItem(
		keyName = HOTKEY,
		name = "Key",
		description = "Key to hold. Ctrl or Alt work best: Shift also turns left-click into Drop if shift-click drop is on, and a non-modifier key can't be typed while it's bound.",
		position = 12,
		section = hotkeySection
	)
	default Keybind hotkey()
	{
		return Keybind.CTRL;
	}
}
