package com.prayerpointfraction;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("prayerPointFraction")
public interface PrayerPointFractionConfig extends Config
{
	@ConfigItem(
		keyName = "currentTick",
		name = "Current tick",
		description = "Test variable"
	)
	default int showCurrentTick()
	{
		return 10;
	}
}
