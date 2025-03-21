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

	@ConfigItem(
		keyName = "setCurrentTick",
		name = "Set current tick",
		description = "Set current tick to value above."
	)
	default boolean setCurrentTick()
	{
		return false;
	}

	@ConfigItem(
			keyName = "setDebug",
			name = "Set debug",
			description = "Set debug mode."
	)
	default boolean setDebug() {
		return false;
	}

}
