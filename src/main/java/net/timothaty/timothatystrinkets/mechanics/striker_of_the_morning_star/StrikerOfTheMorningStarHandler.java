package net.timothaty.timothatystrinkets.mechanics.striker_of_the_morning_star;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.util.ConcussiveStrikeData;
import net.timothaty.timothatystrinkets.util.StrikerOfTheMorningStarCurios;
import net.timothaty.timothatystrinkets.util.StrikerOfTheMorningStarData;
import net.timothaty.timothatystrinkets.util.StrikerOfTheMorningStarEffects;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsEquipState;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsShockwaveHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunTags;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class StrikerOfTheMorningStarHandler {
	private static final long SAFETY_SYNC_INTERVAL_TICKS = 40L;

	private StrikerOfTheMorningStarHandler() {
	}


	public static void onCurioEquip(Player player, ItemStack stack) {
		if (player == null || player.level().isClientSide())
			return;
		TimothatysTrinketsEquipState.set(player, TimothatysTrinketsEquipState.STRIKER_OF_THE_MORNING_STAR, true);
	}

	public static void onCurioUnequip(Player player, ItemStack stack) {
		if (player == null || player.level().isClientSide())
			return;
		TimothatysTrinketsEquipState.set(player, TimothatysTrinketsEquipState.STRIKER_OF_THE_MORNING_STAR, false);
		player.getPersistentData().remove(StrikerOfTheMorningStarData.NBT_SPRINT_START_TICK);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		if (player == null)
			return;

		Level level = player.level();
		if (level.isClientSide())
			return;

		long now = level.getGameTime();
		if ((now % SAFETY_SYNC_INTERVAL_TICKS) == 0L) {
			boolean actual = StrikerOfTheMorningStarCurios.isStrikerEquipped(player);
			boolean cached = TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.STRIKER_OF_THE_MORNING_STAR);
			if (actual != cached) {
				if (actual) {
					onCurioEquip(player, StrikerOfTheMorningStarCurios.getEquippedStrikerStack(player));
				} else {
					onCurioUnequip(player, ItemStack.EMPTY);
				}
			}
		}

		if (!TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.STRIKER_OF_THE_MORNING_STAR)) {
			player.getPersistentData().remove(StrikerOfTheMorningStarData.NBT_SPRINT_START_TICK);
			return;
		}

		if (player.isSprinting()) {
			if (player.getPersistentData().getLong(StrikerOfTheMorningStarData.NBT_SPRINT_START_TICK) <= 0L) {
				player.getPersistentData().putLong(StrikerOfTheMorningStarData.NBT_SPRINT_START_TICK, now);
			}
		} else {
			player.getPersistentData().remove(StrikerOfTheMorningStarData.NBT_SPRINT_START_TICK);
		}
	}

	@SubscribeEvent
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event == null || event.getNewDamage() <= 0.0F)
			return;

		LivingEntity target = event.getEntity();
		if (target == null || !target.isAlive())
			return;

		DamageSource source = event.getSource();
		if (!isPlayerMeleeAttack(source))
			return;

		Entity attackerEntity = source.getEntity();
		if (!(attackerEntity instanceof Player attacker))
			return;
		if (attacker.level().isClientSide())
			return;
		if (target == attacker)
			return;
		if (attacker.getPersistentData().getBoolean(StrikerOfTheMorningStarData.NBT_SHOCKWAVE_DAMAGE_GUARD))
			return;
		if (!TimothatysTrinketsEquipState.has(attacker, TimothatysTrinketsEquipState.STRIKER_OF_THE_MORNING_STAR))
			return;

		ItemStack weapon = attacker.getMainHandItem();
		if (weapon.is(TimothatysTrinketsModItems.MORGENSHTERN.get()))
			return;
		if (target.hasEffect(TimothatysTrinketsModMobEffects.STUN_IMMUNITY))
			return;

		if (ConcussiveStrikeData.has(weapon, attacker.level()))
			return;

		if (weapon.is(Items.MACE)) {
			handleVanillaMaceFallBash(event, attacker, target);
			return;
		}

		handleHeavyArmsSprintBash(attacker, target, weapon);
	}

	private static void handleHeavyArmsSprintBash(Player attacker, LivingEntity target, ItemStack weapon) {
		if (weapon.isEmpty() || !weapon.is(TimothatysTrinketsStunTags.HEAVY_ARMS))
			return;
		if (!hasSprintedLongEnough(attacker))
			return;
		if (isSprintBashOnCooldown(attacker))
			return;
		if (attacker.getRandom().nextFloat() >= StrikerOfTheMorningStarData.SPRINT_BASH_CHANCE)
			return;

		boolean applied = TimothatysTrinketsStunHelper.tryApplyStun(target, attacker, TimothatysTrinketsStunHelper.DEFAULT_SPRINT_BASH_STUN_TICKS);
		if (applied) {
			StrikerOfTheMorningStarEffects.applyStunFatigue(attacker);
			long now = attacker.level().getGameTime();
			attacker.getPersistentData().putLong(StrikerOfTheMorningStarData.NBT_LAST_BASH_TICK, now);
			attacker.level().playSound(null, attacker.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.45F, 0.9F);
		}
	}

	private static void handleVanillaMaceFallBash(LivingDamageEvent.Pre event, Player attacker, LivingEntity target) {
		float fallDistance = attacker.fallDistance;
		if (fallDistance < StrikerOfTheMorningStarData.MACE_MIN_FALL_DISTANCE)
			return;

		int stunTicks = getMaceFallStunTicks(fallDistance);
		boolean applied = TimothatysTrinketsStunHelper.tryApplyStun(target, attacker, stunTicks);
		if (applied) {
			StrikerOfTheMorningStarEffects.applyStunFatigue(attacker);
		}

		if (fallDistance >= StrikerOfTheMorningStarData.MACE_SHOCKWAVE_FALL_DISTANCE && !isMaceShockwaveOnThisTick(attacker)) {
			long now = attacker.level().getGameTime();
			attacker.getPersistentData().putLong(StrikerOfTheMorningStarData.NBT_LAST_MACE_WAVE_TICK, now);

			double radius = fallDistance * StrikerOfTheMorningStarData.MACE_SHOCKWAVE_RADIUS_MULTIPLIER;
			float baseDamage = event.getNewDamage() * StrikerOfTheMorningStarData.MACE_SHOCKWAVE_DAMAGE_MULTIPLIER;
			TimothatysTrinketsShockwaveHelper.createMaceShockwave(attacker, target, radius, baseDamage);
		}
	}

	private static int getMaceFallStunTicks(float fallDistance) {
		int bonusSteps = (int) Math.floor((fallDistance - StrikerOfTheMorningStarData.MACE_MIN_FALL_DISTANCE) / 5.0F);
		return 20 + Math.max(0, bonusSteps) * 4;
	}

	private static boolean hasSprintedLongEnough(Player player) {
		long start = player.getPersistentData().getLong(StrikerOfTheMorningStarData.NBT_SPRINT_START_TICK);
		if (start <= 0L)
			return false;
		return player.level().getGameTime() - start >= StrikerOfTheMorningStarData.MIN_SPRINT_TICKS;
	}

	private static boolean isSprintBashOnCooldown(Player player) {
		long last = player.getPersistentData().getLong(StrikerOfTheMorningStarData.NBT_LAST_BASH_TICK);
		return last > 0L && player.level().getGameTime() - last < StrikerOfTheMorningStarData.SPRINT_BASH_COOLDOWN_TICKS;
	}

	private static boolean isMaceShockwaveOnThisTick(Player player) {
		return player.getPersistentData().getLong(StrikerOfTheMorningStarData.NBT_LAST_MACE_WAVE_TICK) == player.level().getGameTime();
	}

	private static boolean isPlayerMeleeAttack(DamageSource source) {
		if (source == null)
			return false;
		if (!source.is(DamageTypes.PLAYER_ATTACK))
			return false;
		Entity attacker = source.getEntity();
		Entity direct = source.getDirectEntity();
		return attacker instanceof Player && direct == attacker;
	}
}
