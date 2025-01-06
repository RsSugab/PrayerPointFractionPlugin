package com.prayerpointfraction;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "Prayer Point Fraction"
)
public class PrayerPointFractionPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private PrayerPointFractionConfig config;

	private PrayerPointFractionCounter counter;

	@Override
	protected void startUp() throws Exception
	{
		//log.info("Example started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		//log.info("Example stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			//client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Tick is " + config.showCurrentTick(), null);
			//addCounter();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		removeCounter();
		addCounter();
	}


	private void addCounter()
	{
		//TODO: Find prayer icon image
		BufferedImage image = itemManager.getImage(ItemID.PRAYER_XP);
		//TODO: Remove fixed variable
		counter = new PrayerPointFractionCounter(image, this, config.showCurrentTick());

		infoBoxManager.addInfoBox(counter);
	}

	private void removeCounter()
	{
		if (counter == null)
		{
			return;
		}

		infoBoxManager.removeInfoBox(counter);
		counter = null;
	}

	@Provides
	PrayerPointFractionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PrayerPointFractionConfig.class);
	}
}
