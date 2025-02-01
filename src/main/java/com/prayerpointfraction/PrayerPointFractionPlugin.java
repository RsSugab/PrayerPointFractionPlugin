package com.prayerpointfraction;

import com.google.inject.Provides;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.SkillIconManager;
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
	private SkillIconManager skillIconManager;

	@Inject
	private PrayerPointFractionConfig config;

	private PrayerPointFractionCounter counter;

	//TODO: Find how to include this
	//@Getter
	//private final PrayerType prayerType;
	@AllArgsConstructor
	@Getter
	enum PrayerType
	{
		THICK_SKIN("Thick Skin", Prayer.THICK_SKIN, "+5% Defence", SpriteID.PRAYER_THICK_SKIN, false, 1),
		BURST_OF_STRENGTH("Burst of Strength", Prayer.BURST_OF_STRENGTH, "+5% Strength", SpriteID.PRAYER_BURST_OF_STRENGTH, false, 1),
		CLARITY_OF_THOUGHT("Clarity of Thought", Prayer.CLARITY_OF_THOUGHT, "+5% Attack", SpriteID.PRAYER_CLARITY_OF_THOUGHT, false, 1),
		SHARP_EYE("Sharp Eye", Prayer.SHARP_EYE, "+5% Ranged", SpriteID.PRAYER_SHARP_EYE, false, 1),
		MYSTIC_WILL("Mystic Will", Prayer.MYSTIC_WILL, "+5% Magical attack and defence", SpriteID.PRAYER_MYSTIC_WILL, false, 1),
		ROCK_SKIN("Rock Skin", Prayer.ROCK_SKIN, "+10% Defence", SpriteID.PRAYER_ROCK_SKIN, false, 6),
		SUPERHUMAN_STRENGTH("Superhuman Strength", Prayer.SUPERHUMAN_STRENGTH, "+10% Strength", SpriteID.PRAYER_SUPERHUMAN_STRENGTH, false, 6),
		IMPROVED_REFLEXES("Improved Reflexes", Prayer.IMPROVED_REFLEXES, "+10% Attack", SpriteID.PRAYER_IMPROVED_REFLEXES, false, 6),
		RAPID_RESTORE("Rapid Restore", Prayer.RAPID_RESTORE, "2 x Restore rate for all skills except Hitpoints and Prayer", SpriteID.PRAYER_RAPID_RESTORE, false, 1),
		RAPID_HEAL("Rapid Heal", Prayer.RAPID_HEAL, "2 x Restore rate for Hitpoints", SpriteID.PRAYER_RAPID_HEAL, false, 2),
		PROTECT_ITEM("Protect Item", Prayer.PROTECT_ITEM, "Player keeps 1 extra item when they die", SpriteID.PRAYER_PROTECT_ITEM, false, 2),
		HAWK_EYE("Hawk Eye", Prayer.HAWK_EYE, "+10% Ranged", SpriteID.PRAYER_HAWK_EYE, false, 6),
		MYSTIC_LORE("Mystic Lore", Prayer.MYSTIC_LORE, "+10% Magical attack and defence", SpriteID.PRAYER_MYSTIC_LORE, false, 6),
		STEEL_SKIN("Steel Skin", Prayer.STEEL_SKIN, "+15% Defence", SpriteID.PRAYER_STEEL_SKIN, false, 12),
		ULTIMATE_STRENGTH("Ultimate Strength", Prayer.ULTIMATE_STRENGTH, "+15% Strength", SpriteID.PRAYER_ULTIMATE_STRENGTH, false, 12),
		INCREDIBLE_REFLEXES("Incredible reflexes", Prayer.INCREDIBLE_REFLEXES, "+15% Attack", SpriteID.PRAYER_INCREDIBLE_REFLEXES, false, 12),
		PROTECT_FROM_MAGIC("protect from magic", Prayer.PROTECT_FROM_MAGIC, "Protects against magic attacks", SpriteID.PRAYER_PROTECT_FROM_MAGIC, true, 12),
		PROTECT_FROM_MISSILES("Protect from missiles", Prayer.PROTECT_FROM_MISSILES, "Protects against ranged attacks", SpriteID.PRAYER_PROTECT_FROM_MISSILES, true, 12),
		PROTECT_FROM_MELEE("Protect from melee", Prayer.PROTECT_FROM_MELEE, "Protects against melee attacks", SpriteID.PRAYER_PROTECT_FROM_MELEE, true, 12),
		EAGLE_EYE("Eagle Eye", Prayer.EAGLE_EYE, "+15% Ranged", SpriteID.PRAYER_EAGLE_EYE, false, 12),
		MYSTIC_MIGHT("Mystic Might", Prayer.MYSTIC_MIGHT, "+15% Magical attack and defence", SpriteID.PRAYER_MYSTIC_MIGHT, false, 12),
		RETRIBUTION("Retribution", Prayer.RETRIBUTION, "Deals damage up to 25% of your Prayer level to nearby targets upon the user's death", SpriteID.PRAYER_RETRIBUTION, true, 3),
		REDEMPTION("Redemption", Prayer.REDEMPTION, "Heals the player if they fall below 10% health", SpriteID.PRAYER_REDEMPTION, true, 6),
		SMITE("Smite", Prayer.SMITE, "Removes 1 Prayer point from an enemy for every 4 damage inflicted on the enemy", SpriteID.PRAYER_SMITE, true, 18),
		PRESERVE("Preserve", Prayer.PRESERVE, "Boosted stats last 50% longer", SpriteID.PRAYER_PRESERVE, false, 2),
		CHIVALRY("Chivalry", Prayer.CHIVALRY, "+15% Attack, +18% Strength, +20% Defence", SpriteID.PRAYER_CHIVALRY, false, 24),
		PIETY("Piety", Prayer.PIETY, "+20% Attack, +23% Strength, +25% Defence", SpriteID.PRAYER_PIETY, false, 24),
		RIGOUR("Rigour", Prayer.RIGOUR, "+20% Ranged attack, +23% Ranged strength, +25% Defence", SpriteID.PRAYER_RIGOUR, false, 24),
		AUGURY("Augury", Prayer.AUGURY, "+25% Magical attack and defence, +25% Defence", SpriteID.PRAYER_AUGURY, false, 24),
		;

		private final String name;
		private final Prayer prayer;
		private final String description;
		private final int spriteID;
		private final boolean overhead;
		private final int drainEffect;
	}

	private int prayerBonus;
	private int prayerDrainCounter;
	//TODO: Generalize for all prayers
	private boolean thickSkinFlicked;

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
			//TODO: Could find a way to store between logins
			prayerDrainCounter = 0;
			//prayerDrainCounter = config.showCurrentTick();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		updateDrainCounter();
		removeDrainInfobox();
		addPrayerDrainInfobox(prayerDrainCounter);
	}

	private void updateDrainCounter()
	{
//		//currentTick goes from 1 to config.showCurrentTick()
//		if (prayerDrainCounter <= 1)
//		{
//			prayerDrainCounter = config.showCurrentTick();
//			return;
//		}
//
//		prayerDrainCounter--;

		//TODO: Generalize for all prayers
		if (!thickSkinFlicked)
		{
			prayerDrainCounter+=getDrainEffect(client);
		}
		thickSkinFlicked = false;

		int prayerDrainThreshold = 60 + prayerBonus*2;
		if (prayerDrainCounter >= prayerDrainThreshold)
		{
			//TODO: Confirm if the counter goes to 0 or is just reduced by prayerDrainThreshold
			prayerDrainCounter-=prayerDrainThreshold;
		}
	}

	//Taken from Prayer plugin
	private static int getDrainEffect(Client client)
	{
		int drainEffect = 0;

		for (PrayerType prayerType : PrayerType.values())
		{
			if (client.isPrayerActive(prayerType.prayer))
			{
				drainEffect += prayerType.drainEffect;
			}
		}

		return drainEffect;
	}

	private void addPrayerDrainInfobox(int value)
	{
		BufferedImage image = skillIconManager.getSkillImage(Skill.PRAYER);
		counter = new PrayerPointFractionCounter(image, this, value, 60 + prayerBonus*2);
		infoBoxManager.addInfoBox(counter);
	}

	private void removeDrainInfobox()
	{
		if (counter == null)
		{
			return;
		}

		infoBoxManager.removeInfoBox(counter);
		counter = null;
	}

	//Taken from the Prayer plugin
	private int totalPrayerBonus(Item[] items)
	{
		int total = 0;
		for (Item item : items)
		{
			ItemStats is = itemManager.getItemStats(item.getId());
			if (is != null && is.getEquipment() != null)
			{
				total += is.getEquipment().getPrayer();
			}
		}
		return total;
	}

	@Subscribe
	public void onItemContainerChanged(final ItemContainerChanged event)
	{
		final int id = event.getContainerId();
		if (id == InventoryID.EQUIPMENT.getId())
		{
			prayerBonus = totalPrayerBonus(event.getItemContainer().getItems());
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		//TODO: Make it work for all prayers
		if (event.getVarbitId() == Varbits.PRAYER_THICK_SKIN)
		{
			//TODO: Keep note if prayer was flicked
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Flicked this tick", null);
			thickSkinFlicked = true;
		}
	}


	@Provides
	PrayerPointFractionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PrayerPointFractionConfig.class);
	}
}
