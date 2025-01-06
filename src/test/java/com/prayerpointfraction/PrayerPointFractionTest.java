package com.prayerpointfraction;

import com.prayerpointfraction.PrayerPointFractionPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PrayerPointFractionTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PrayerPointFractionPlugin.class);
		RuneLite.main(args);
	}
}