package com.prayerpointfraction;

//import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.*;

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

	private PrayerPointFractionCounter thresholdCounter;
	private PrayerPointFractionCounter ticksCounter;

	private int lastPrayerDrainEffect;

	private Queue<PrayerEventQueue> prayerEventQueue = new LinkedList<>();

	//TODO: Remove debug
	boolean debug = false;

	public enum TickFlickStatus
	{
		PRAYER_INACTIVE,
		PRAYER_FICKED,
		PRAYER_DRAINED;
	}

	@AllArgsConstructor
	@Getter
	enum PrayerType
	{
		//TODO: Add new prayers and cleanup
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
	private int prayerTicksCounter;

	private boolean flagRecalculateTicksLeft;
	private boolean flagNoPrayerActiveLastTick;

	private TickFlickStatus tickFlickStatus;

	private TickFlickStatus prayerFlickedFlag[] = new TickFlickStatus[PrayerType.values().length];

	@Override
	protected void startUp() throws Exception
	{
		//log.info("Example started!");
		prayerDrainCounter = 0;
		flagRecalculateTicksLeft = true;
		addPrayerTicksInfobox(0);
	}

	@Override
	protected void shutDown() throws Exception
	{
		//log.info("Example stopped!");
		//TODO: Add remove infobox
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
//		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
//		{
//			//client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Tick is " + config.showCurrentTick(), null);
//			//addCounter();
//			//TODO: Could find a way to store between logins
//			prayerDrainCounter = 0;
//			//prayerDrainCounter = config.showCurrentTick();
//		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		debug = config.setDebug();;
		updateDrainCounter();
		if (debug)
		{
			String message = "Tick: " + client.getTickCount();
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
			while (!prayerEventQueue.isEmpty())
			{
				PrayerEventQueue queueEvent = prayerEventQueue.remove();
				message = "Prayer: " + queueEvent.getPrayerType().getName() + " Queue varbit: " + queueEvent.isPrayerActivated();
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
			}

			if (thresholdCounter == null)
			{
				addPrayerDrainInfobox(prayerDrainCounter);
			}
			else
			{
				thresholdCounter.setCount(prayerDrainCounter);
				thresholdCounter.setTickFlickStatus(tickFlickStatus);
			}
		}

		updateTicksCounter();
		if (ticksCounter == null)
		{
			addPrayerTicksInfobox(prayerTicksCounter);
		}
		else
		{
			ticksCounter.setPrayerDrainThreshold(60 + prayerBonus*2);
			ticksCounter.setPrayerDrainCounter(prayerDrainCounter);
			ticksCounter.setCount(prayerTicksCounter);
			ticksCounter.setTickFlickStatus(tickFlickStatus);
		}
		tickFlickStatus = TickFlickStatus.PRAYER_INACTIVE;
	}

	private void updateDrainCounter()
	{
		int drainEffect = getDrainEffect(client);
		if (tickFlickStatus == TickFlickStatus.PRAYER_DRAINED)
		{
			prayerDrainCounter += drainEffect;
		}
//		if (drainEffect > 0)
//		{
//			prayerDrainCounter += drainEffect;
//			tickFlickStatus = TickFlickStatus.PRAYER_DRAINED;
//		}

		int prayerDrainThreshold = 60 + prayerBonus*2;
		if (prayerDrainCounter > prayerDrainThreshold)
		{
			prayerDrainCounter-=prayerDrainThreshold;
		}
		if (debug)
		{
			if (config.setCurrentTick()) {
				prayerDrainCounter = config.showCurrentTick();
			}
		}
	}

	private void updateTicksCounter()
	{
		int ticksPrayerActive = getAmountTicksPrayerActive(client);
		// Keep number of ticks based on the last active prayers
		if (ticksPrayerActive > 0)
		{
			prayerTicksCounter = ticksPrayerActive;
		} else if (flagRecalculateTicksLeft)
		{
			prayerTicksCounter = calculateTicksPrayerActive(lastPrayerDrainEffect);
		}
		if (flagRecalculateTicksLeft && lastPrayerDrainEffect == 0)
		{
			prayerTicksCounter = -1;
		}
		flagRecalculateTicksLeft = false;
	}

	//Inspired from Prayer plugin
	private int getDrainEffect(Client client)
	{
		int drainEffect = 0;
		Set<String> activatedPrayers = new HashSet<String>();
		tickFlickStatus = TickFlickStatus.PRAYER_INACTIVE;

		for (PrayerType prayerType : PrayerType.values())
		{
			if (client.isPrayerActive(prayerType.prayer))
			{
				activatedPrayers.add(prayerType.getName());
				drainEffect += prayerType.drainEffect;
			}
//			if (client.isPrayerActive(prayerType.prayer))
//			{
//				if(prayerFlickedFlag[prayerType.ordinal()] == TickFlickStatus.PRAYER_DRAINED)
//				{
//					tickFlickStatus = TickFlickStatus.PRAYER_DRAINED;
//				}
//				else
//				{
//					if (tickFlickStatus == TickFlickStatus.PRAYER_INACTIVE)
//					{
//						tickFlickStatus = TickFlickStatus.PRAYER_FICKED;
//					}
//				}
//				// Set all active prayers to PRAYER_DRAINED for next tick calculations
//				prayerFlickedFlag[prayerType.ordinal()] = TickFlickStatus.PRAYER_DRAINED;
//				drainEffect += prayerType.drainEffect;
//			}
//			else
//			{
//				if (prayerFlickedFlag[prayerType.ordinal()] == TickFlickStatus.PRAYER_DRAINED)
//				{
//					// Prayer got forced removed (eg. switching overhead)
//					tickFlickStatus = TickFlickStatus.PRAYER_DRAINED;
//				}
//				prayerFlickedFlag[prayerType.ordinal()] = TickFlickStatus.PRAYER_INACTIVE;
//			}
		}
		if(debug)
		{
			String[] array = activatedPrayers.toArray(new String[activatedPrayers.size()]);
			for (String prayer : array)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Activated prayer: " + prayer, null);
			}
		}

		// We keep going through events to see if we deactivated all active prayers before activating another prayer
		// to determine if we did a prayer flick
//		while (!prayerEventQueue.isEmpty())
//		{
//			PrayerEventQueue prayerEvent = prayerEventQueue.remove();
//			if (prayerEvent.isPrayerActivated())
//			{
//				//Check for sequence Activate > Deactivate > Activate of the same prayer
//				PrayerEventQueue secondPrayerEvent = prayerEventQueue.peek();
//				if(secondPrayerEvent != null
//						&& !secondPrayerEvent.isPrayerActivated()
//						&& prayerEvent.getPrayerType().getName().equals(secondPrayerEvent.getPrayerType().getName()))
//				{
//					prayerEventQueue.remove();
//					PrayerEventQueue thirdEvent = prayerEventQueue.peek();
//					if(thirdEvent != null
//							&& thirdEvent.isPrayerActivated()
//							&& prayerEvent.getPrayerType().getName().equals(thirdEvent.getPrayerType().getName()))
//					{
//						prayerEventQueue.remove();
//						activatedPrayers.remove(prayerEvent.getPrayerType());
//					}
//				}
//				else
//				{
//					break;
//				}
//			}
//			else
//			{
//				activatedPrayers.remove(prayerEvent.getPrayerType());
//
//				// Sequence can also be Deactivated > Activated
//				PrayerEventQueue secondPrayerEvent = prayerEventQueue.peek();
//				if(secondPrayerEvent != null
//						&& !secondPrayerEvent.isPrayerActivated()
//						&& prayerEvent.getPrayerType().getName().equals(secondPrayerEvent.getPrayerType().getName()))
//				{
//					prayerEventQueue.remove();
//				}
//			}
//
//		}
		tickFlickStatus = TickFlickStatus.PRAYER_DRAINED;
		// TODO: Idea: Start with prayer active set, add and remove from set according to queue, check if set is  ever empty
		if (flagNoPrayerActiveLastTick && !activatedPrayers.isEmpty())
		{
			tickFlickStatus = TickFlickStatus.PRAYER_FICKED;
		}


		if (activatedPrayers.isEmpty())
		{
			tickFlickStatus = TickFlickStatus.PRAYER_FICKED;
			flagNoPrayerActiveLastTick = true;
		}
		else
		{
			flagNoPrayerActiveLastTick = false;
		}
		while (!prayerEventQueue.isEmpty())
		{
			PrayerEventQueue prayerEvent = prayerEventQueue.remove();
			if (prayerEvent.isPrayerActivated())
			{
				boolean ret = activatedPrayers.add(prayerEvent.getPrayerType().getName());
				if (debug)
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Added " + ret + " " + prayerEvent.getPrayerType().getName() , null);
			}
			else
			{
				boolean ret = activatedPrayers.remove(prayerEvent.getPrayerType().getName());
				if (debug)
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Removed " + ret + " " + prayerEvent.getPrayerType().getName() , null);
			}
			if(debug)
			{
				String[] array = activatedPrayers.toArray(new String[activatedPrayers.size()]);
				String message = "Queue: ";
				for (String prayer : array)
				{
					message += prayer + " ";
				}
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
			}
			if (activatedPrayers.isEmpty())
			{
				tickFlickStatus = TickFlickStatus.PRAYER_FICKED;
			}

		}

		if (drainEffect == 0)
		{
			tickFlickStatus = TickFlickStatus.PRAYER_INACTIVE;
		}
//		else if (activatedPrayers.isEmpty() || flagNoPrayerActiveLastTick)
//		{
//			tickFlickStatus = TickFlickStatus.PRAYER_FICKED;
//			drainEffect = 0;
//			flagNoPrayerActiveLastTick = false;
//		}
//		else
//		{
//			tickFlickStatus = TickFlickStatus.PRAYER_DRAINED;
//		}
		if (debug)
		{
			String message;
			if (tickFlickStatus == TickFlickStatus.PRAYER_DRAINED)
				message = "DRAINED";
			else if (tickFlickStatus == TickFlickStatus.PRAYER_INACTIVE)
				message = "inactive";
			else
				message = "Flicked";
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "***Prayer was: " + message, null);
		}

//		if (tickFlickStatus == TickFlickStatus.PRAYER_FICKED)
//		{
//			drainEffect = 0;
//		}

//		while (!prayerEventQueue.isEmpty())
//		{
//			prayerEventQueue.remove();
//		}

		return drainEffect;
	}

	//Inspired from Prayer plugin
	private int getAmountTicksPrayerActive(Client client)
	{
		int drainEffectNoFlick = 0;

		for (PrayerType prayerType : PrayerType.values())
		{
			if (client.isPrayerActive(prayerType.prayer))
			{
				drainEffectNoFlick += prayerType.drainEffect;
			}
		}

		int ticksPrayerActive = 0;

		if (drainEffectNoFlick > 0)
		{
			ticksPrayerActive = calculateTicksPrayerActive(drainEffectNoFlick);
			lastPrayerDrainEffect = drainEffectNoFlick;
		}

		return ticksPrayerActive;
	}

	private int calculateTicksPrayerActive(int drainEffect)
	{
		int prayerDrainThreshold = 60 + prayerBonus*2;
		int offset = 0;
		//

		return ((prayerDrainThreshold-prayerDrainCounter)/drainEffect) + 1;
	}

	private void addPrayerDrainInfobox(int value)
	{
		BufferedImage image = skillIconManager.getSkillImage(Skill.PRAYER);
		thresholdCounter = new PrayerPointFractionCounter(image, this, value, 60 + prayerBonus*2, prayerDrainCounter, tickFlickStatus);
		infoBoxManager.addInfoBox(thresholdCounter);
	}

	private void addPrayerTicksInfobox(int value)
	{
		BufferedImage image = skillIconManager.getSkillImage(Skill.PRAYER);
		ticksCounter = new PrayerPointFractionCounter(image, this, value, 60 + prayerBonus*2, prayerDrainCounter, tickFlickStatus);
		infoBoxManager.addInfoBox(ticksCounter);
	}

	private void removeDrainInfobox()
	{
		if (thresholdCounter == null)
		{
			return;
		}

		infoBoxManager.removeInfoBox(thresholdCounter);
		thresholdCounter = null;
	}

	private void removeTicksInfobox()
	{
		if (ticksCounter == null)
		{
			return;
		}

		infoBoxManager.removeInfoBox(ticksCounter);
		ticksCounter = null;
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
	public void onAnimationChanged(AnimationChanged animationChanged)
	{
		final Actor actor = animationChanged.getActor();
		final int anim = actor.getAnimation();

		//TODO: Investigate if other actions (linked to animations) reset prayerDrainCounter
		if (anim == 7305 //POH pool
				|| anim == 645) //Altar
		{
			prayerDrainCounter = 0;
			flagRecalculateTicksLeft = true;
			ticksCounter.initialize();
			if(debug)
			{
				thresholdCounter.initialize();
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(final ItemContainerChanged event)
	{
		final int id = event.getContainerId();
		if (id == InventoryID.EQUIPMENT.getId())
		{
			prayerBonus = totalPrayerBonus(event.getItemContainer().getItems());
		}
		flagRecalculateTicksLeft = true;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if(event.isItemOp())
		{
			// Falador shield 3 and 4 resets prayer drain counter
			if(event.getItemId() == 13119 || event.getItemId() == 13120)
			{
				if (event.getMenuOption().equals("Recharge-prayer"))
				{
					// Falador shield only restores prayer if you're missing prayer points
					if (client.getBoostedSkillLevel(Skill.PRAYER) < client.getRealSkillLevel(Skill.PRAYER))
					{
						prayerDrainCounter = 0;
						flagRecalculateTicksLeft = true;
						ticksCounter.initialize();
						if(debug)
						{
							thresholdCounter.initialize();
						}
					}
				}
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		for (PrayerType prayerType : PrayerType.values())
		{
			if (event.getVarbitId() == prayerType.getPrayer().getVarbit())
			{
				if (debug)
				{
					//TODO: Keep note if prayer was flicked
					String message = prayerType.getName() + ": " + event.getValue();
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
				}
				prayerEventQueue.add(new PrayerEventQueue(prayerType, event.getValue()==1));
//				// Prayer got deactivated
//				if (event.getValue() == 0)
//				{
//					if (!flagPrayerGotForcedRemoved)
//					{
//						prayerFlickedFlag[prayerType.ordinal()] = TickFlickStatus.PRAYER_INACTIVE;
//					}
//					else
//					{
//						//TODO: Test this scenario
//						prayerFlickedFlag[prayerType.ordinal()] = TickFlickStatus.PRAYER_DRAINED;
//					}
//				}
//				else
//				{
//					// Prayer got activated
//					if (!flagPrayerGotForcedRemoved)
//					{
//
//					}
//					else
//					{
//						//TODO: Test this scenario
//						prayerFlickedFlag[prayerType.ordinal()] = TickFlickStatus.PRAYER_DRAINED;
//					}
//				}
//				prayerFlickedFlag[prayerType.ordinal()] = true;
			}
		}

	}


	@Provides
	PrayerPointFractionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PrayerPointFractionConfig.class);
	}
}
