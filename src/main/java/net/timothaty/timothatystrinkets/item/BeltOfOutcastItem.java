package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.util.TimothatysCuriosHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsEquipState;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsAttributeHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public class BeltOfOutcastItem extends Item {
	private static final float DISCOUNT_PERCENT = 0.20f;
	private static final int HAPPY_PARTICLE_CHANCE_TICKS = 120;
	private static final long HERO_CHECK_INTERVAL_TICKS = 20L;
	private static final long SAFETY_SYNC_INTERVAL_TICKS = 40L;
	private static final double OUTSIDE_VILLAGE_SPEED_BONUS = 0.05D;
	private static final ResourceLocation OUTSIDE_VILLAGE_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "belt_of_outcast_outside_village_speed");

	private static final String NBT_BOOSTED = "ttr_belt_hero_boosted";
	private static final String NBT_ORIG_AMP = "ttr_belt_hero_orig_amp";
	private static final String NBT_ORIG_AMBIENT = "ttr_belt_hero_orig_ambient";
	private static final String NBT_ORIG_VISIBLE = "ttr_belt_hero_orig_visible";
	private static final String NBT_ORIG_ICON = "ttr_belt_hero_orig_icon";
	private static final String NBT_LAST_DURATION = "ttr_belt_hero_last_duration";

	public BeltOfOutcastItem() {
		super(new Item.Properties().stacksTo(1));
	}

	public static void onCurioEquip(Player player, ItemStack stack) {
		if (player == null || player.level().isClientSide())
			return;
		TimothatysTrinketsEquipState.set(player, TimothatysTrinketsEquipState.BELT_OF_OUTCAST, true);
		tickHeroBoost(player);
		updateOutsideVillageSpeed(player, true);
	}

	public static void onCurioUnequip(Player player, ItemStack stack) {
		if (player == null || player.level().isClientSide())
			return;
		TimothatysTrinketsEquipState.set(player, TimothatysTrinketsEquipState.BELT_OF_OUTCAST, false);
		tickHeroBoost(player);
		updateOutsideVillageSpeed(player, false);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player == null || player.level().isClientSide)
			return;

		long now = player.level().getGameTime();
		if ((now % SAFETY_SYNC_INTERVAL_TICKS) == 0L) {
			syncEquipState(player);
		}

		boolean hasBelt = TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.BELT_OF_OUTCAST);
		if (hasBelt) {
			trySpawnHappyParticles(player);
		}

		if ((now % HERO_CHECK_INTERVAL_TICKS) != 0L)
			return;

		tickHeroBoost(player);
		updateOutsideVillageSpeed(player, hasBelt);
	}

	private static void updateOutsideVillageSpeed(Player player, boolean hasBelt) {
		boolean shouldBoost = hasBelt
				&& player.level() instanceof ServerLevel serverLevel
				&& !serverLevel.isVillage(player.blockPosition());
		TimothatysTrinketsAttributeHelper.setModifier(
				player,
				Attributes.MOVEMENT_SPEED,
				OUTSIDE_VILLAGE_SPEED_ID,
				OUTSIDE_VILLAGE_SPEED_BONUS,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				shouldBoost
		);
	}

	@SubscribeEvent
	public static void onInteractWithVillager(PlayerInteractEvent.EntityInteractSpecific event) {
		Player player = event.getEntity();
		if (player == null || player.level().isClientSide)
			return;
		if (!(event.getTarget() instanceof AbstractVillager villager))
			return;
		if (!TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.BELT_OF_OUTCAST))
			return;
		if (player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE))
			return;

		applyExtraDiscount(villager, player);
	}

	private static void syncEquipState(Player player) {
		boolean actual = TimothatysCuriosHelper.hasCurio(player, TimothatysTrinketsModItems.BELT_OF_OUTCAST.get());
		boolean cached = TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.BELT_OF_OUTCAST);
		if (actual == cached)
			return;
		if (actual) {
			onCurioEquip(player, TimothatysCuriosHelper.findCurio(player, TimothatysTrinketsModItems.BELT_OF_OUTCAST.get()));
		} else {
			onCurioUnequip(player, ItemStack.EMPTY);
		}
	}

	private static void tickHeroBoost(Player player) {
		boolean hasBelt = TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.BELT_OF_OUTCAST);
		MobEffectInstance hero = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
		boolean boosted = player.getPersistentData().getBoolean(NBT_BOOSTED);

		if (hero == null) {
			if (boosted) {
				if (!hasBelt) {
					int lastDur = player.getPersistentData().getInt(NBT_LAST_DURATION);
					if (lastDur > 0) {
						int origAmp = player.getPersistentData().getInt(NBT_ORIG_AMP);
						boolean amb = player.getPersistentData().getBoolean(NBT_ORIG_AMBIENT);
						boolean vis = player.getPersistentData().getBoolean(NBT_ORIG_VISIBLE);
						boolean ico = player.getPersistentData().getBoolean(NBT_ORIG_ICON);
						forceSetHero(player, lastDur, origAmp, amb, vis, ico);
					}
				}
				clearHeroBoostData(player);
			}
			return;
		}

		if (boosted) {
			player.getPersistentData().putInt(NBT_LAST_DURATION, hero.getDuration());
		}

		if (hasBelt) {
			if (!boosted) {
				player.getPersistentData().putInt(NBT_ORIG_AMP, hero.getAmplifier());
				player.getPersistentData().putBoolean(NBT_ORIG_AMBIENT, hero.isAmbient());
				player.getPersistentData().putBoolean(NBT_ORIG_VISIBLE, hero.isVisible());
				player.getPersistentData().putBoolean(NBT_ORIG_ICON, hero.showIcon());
				player.getPersistentData().putInt(NBT_LAST_DURATION, hero.getDuration());
				player.getPersistentData().putBoolean(NBT_BOOSTED, true);
				forceSetHero(player, hero.getDuration(), hero.getAmplifier() + 1, hero.isAmbient(), hero.isVisible(), hero.showIcon());
			} else {
				int expected = player.getPersistentData().getInt(NBT_ORIG_AMP) + 1;
				if (hero.getAmplifier() != expected) {
					forceSetHero(player, hero.getDuration(), expected, hero.isAmbient(), hero.isVisible(), hero.showIcon());
				}
			}
			return;
		}

		if (boosted) {
			int origAmp = player.getPersistentData().getInt(NBT_ORIG_AMP);
			boolean amb = player.getPersistentData().getBoolean(NBT_ORIG_AMBIENT);
			boolean vis = player.getPersistentData().getBoolean(NBT_ORIG_VISIBLE);
			boolean ico = player.getPersistentData().getBoolean(NBT_ORIG_ICON);
			forceSetHero(player, hero.getDuration(), origAmp, amb, vis, ico);
			clearHeroBoostData(player);
		}
	}

	private static void trySpawnHappyParticles(Player player) {
		if (!(player.level() instanceof ServerLevel serverLevel))
			return;
		RandomSource rand = player.getRandom();
		if (HAPPY_PARTICLE_CHANCE_TICKS <= 0)
			return;
		if (rand.nextInt(HAPPY_PARTICLE_CHANCE_TICKS) == 0) {
			int count = 1 + rand.nextInt(3);
			serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), count, 0.35, 0.25, 0.35, 0.0);
		}
	}

	private static void forceSetHero(Player player, int duration, int amplifier, boolean ambient, boolean visible, boolean icon) {
		player.removeEffect(MobEffects.HERO_OF_THE_VILLAGE);
		player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, duration, amplifier, ambient, visible, icon));
	}

	private static void clearHeroBoostData(Player player) {
		player.getPersistentData().remove(NBT_BOOSTED);
		player.getPersistentData().remove(NBT_ORIG_AMP);
		player.getPersistentData().remove(NBT_ORIG_AMBIENT);
		player.getPersistentData().remove(NBT_ORIG_VISIBLE);
		player.getPersistentData().remove(NBT_ORIG_ICON);
		player.getPersistentData().remove(NBT_LAST_DURATION);
	}

	private static void applyExtraDiscount(AbstractVillager villager, Player player) {
		try {
			java.lang.reflect.Method m = villager.getClass().getMethod("updateSpecialPrices", Player.class);
			m.invoke(villager, player);
		} catch (Throwable ignored) {
		}

		for (MerchantOffer offer : villager.getOffers()) {
			int baseCount = offer.getBaseCostA().getCount();
			if (baseCount <= 1)
				continue;
			int discount = (int) Math.floor(baseCount * DISCOUNT_PERCENT);
			if (discount < 1)
				discount = 1;
			if (discount >= baseCount)
				discount = baseCount - 1;
			offer.addToSpecialPriceDiff(-discount);
		}
	}
}
