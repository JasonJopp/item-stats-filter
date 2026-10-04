package com.itemstatsfilter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ItemStatsFilterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ItemStatsFilterPlugin.class);
		RuneLite.main(args);
	}
}
