package net.timothaty.timothatystrinkets.mechanics.concussive;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.network.ConcussiveStrikeCameraShakeMessage;
import net.timothaty.timothatystrinkets.util.ConcussiveStrikeData;
import net.timothaty.timothatystrinkets.util.StrikerOfTheMorningStarCurios;
import net.timothaty.timothatystrinkets.util.StrikerOfTheMorningStarData;
import net.timothaty.timothatystrinkets.util.StrikerOfTheMorningStarEffects;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class ConcussiveStrikeEvents {
	private ConcussiveStrikeEvents() {
	}


	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onDirectPlayerAttack(AttackEntityEvent event) {
		if (event == null || event.isCanceled())
			return;

		Player attacker = event.getEntity();
		Entity target = event.getTarget();
		if (attacker == null || target == null || attacker.level().isClientSide())
			return;
		if (!(target instanceof LivingEntity))
			return;

		ItemStack weapon = attacker.getMainHandItem();
		if (weapon.isEmpty())
			return;
		if (!ConcussiveStrikeData.has(weapon, attacker.level()))
			return;

		attacker.getPersistentData().putInt(ConcussiveStrikeData.NBT_LAST_DIRECT_TARGET_ID, target.getId());
		attacker.getPersistentData().putLong(ConcussiveStrikeData.NBT_LAST_DIRECT_ATTACK_TICK, attacker.level().getGameTime());
		attacker.getPersistentData().putBoolean(
				ConcussiveStrikeData.NBT_LAST_ATTACK_FULLY_CHARGED,
				attacker.getAttackStrengthScale(0.5F) >= ConcussiveStrikeData.FULL_ATTACK_STRENGTH
		);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event == null)
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
		if (attacker.level().isClientSide() || target == attacker)
			return;
		if (target.hasEffect(TimothatysTrinketsModMobEffects.STUN_IMMUNITY))
			return;

		ItemStack weapon = attacker.getMainHandItem();
		if (weapon.isEmpty() || !weapon.is(ConcussiveStrikeData.COMPAT_ITEMS))
			return;

		int concussiveLevel = getConcussiveStrikeLevel(weapon, attacker.level());
		if (concussiveLevel <= 0)
			return;
		if (!isEligibleTargetFromCurrentAttack(attacker, target, weapon))
			return;
		if (!TimothatysTrinketsStunHelper.canAttemptControl(target))
			return;

		float strikerBonus = getStrikerConcussiveBonus(attacker, target);
		if (!getOrCreateProcRoll(attacker, target, ConcussiveStrikeData.LISTED_CHANCE + strikerBonus))
			return;

		boolean applied = TimothatysTrinketsStunHelper.tryApplyStunSilently(target, attacker, ConcussiveStrikeData.getStunTicks(concussiveLevel));
		if (applied) {
			if (strikerBonus > 0.0F) {
				StrikerOfTheMorningStarEffects.applyStunFatigue(attacker);
			}
			playConcussiveStrikeSoundOnce(attacker, target);
			shakeCameraOnce(attacker);
		}
	}

	private static boolean isEligibleTargetFromCurrentAttack(Player attacker, LivingEntity target, ItemStack weapon) {
		long attackTick = attacker.getPersistentData().getLong(ConcussiveStrikeData.NBT_LAST_DIRECT_ATTACK_TICK);
		long now = attacker.level().getGameTime();
		if (attackTick != now)
			return false;
		if (!attacker.getPersistentData().getBoolean(ConcussiveStrikeData.NBT_LAST_ATTACK_FULLY_CHARGED))
			return false;

		int directTargetId = attacker.getPersistentData().getInt(ConcussiveStrikeData.NBT_LAST_DIRECT_TARGET_ID);
		if (target.getId() == directTargetId)
			return true;

		return weapon.is(ItemTags.SWORDS) && getSweepingEdgeLevel(weapon, attacker.level()) >= ConcussiveStrikeData.REQUIRED_SWEEPING_EDGE_LEVEL;
	}

	private static boolean getOrCreateProcRoll(Player attacker, LivingEntity target, float chance) {
		long now = attacker.level().getGameTime();
		long rollTick = attacker.getPersistentData().getLong(ConcussiveStrikeData.NBT_PROC_ROLL_TICK);
		int directTargetId = attacker.getPersistentData().getInt(ConcussiveStrikeData.NBT_LAST_DIRECT_TARGET_ID);
		int rollDirectTargetId = attacker.getPersistentData().getInt(ConcussiveStrikeData.NBT_PROC_ROLL_DIRECT_TARGET_ID);
		int targetId = target.getId();
		int rollTargetId = attacker.getPersistentData().getInt(ConcussiveStrikeData.NBT_PROC_ROLL_TARGET_ID);

		if (rollTick == now && rollDirectTargetId == directTargetId && rollTargetId == targetId) {
			return attacker.getPersistentData().getBoolean(ConcussiveStrikeData.NBT_PROC_ROLL_SUCCEEDED);
		}

		boolean succeeded = attacker.getRandom().nextFloat() < chance;
		attacker.getPersistentData().putLong(ConcussiveStrikeData.NBT_PROC_ROLL_TICK, now);
		attacker.getPersistentData().putInt(ConcussiveStrikeData.NBT_PROC_ROLL_DIRECT_TARGET_ID, directTargetId);
		attacker.getPersistentData().putInt(ConcussiveStrikeData.NBT_PROC_ROLL_TARGET_ID, targetId);
		attacker.getPersistentData().putBoolean(ConcussiveStrikeData.NBT_PROC_ROLL_SUCCEEDED, succeeded);
		return succeeded;
	}

	private static int getConcussiveStrikeLevel(ItemStack weapon, Level level) {
		return ConcussiveStrikeData.getLevel(weapon, level);
	}

	private static int getSweepingEdgeLevel(ItemStack weapon, Level level) {
		try {
			Holder<Enchantment> holder = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SWEEPING_EDGE);
			return weapon.getEnchantmentLevel(holder);
		} catch (Exception ignored) {
			return 0;
		}
	}

	private static float getStrikerConcussiveBonus(Player attacker, LivingEntity target) {
		if (!StrikerOfTheMorningStarCurios.isStrikerEquipped(attacker))
			return 0.0F;

		return target instanceof Player
				? StrikerOfTheMorningStarData.CONCUSSIVE_PLAYER_STUN_CHANCE_BONUS
				: StrikerOfTheMorningStarData.CONCUSSIVE_NON_PLAYER_STUN_CHANCE_BONUS;
	}

	private static void playConcussiveStrikeSoundOnce(Player attacker, LivingEntity target) {
		Level level = attacker.level();
		if (level.isClientSide())
			return;

		long now = level.getGameTime();
		int directTargetId = attacker.getPersistentData().getInt(ConcussiveStrikeData.NBT_LAST_DIRECT_TARGET_ID);
		long soundTick = attacker.getPersistentData().getLong(ConcussiveStrikeData.NBT_SOUND_TICK);
		int soundDirectTargetId = attacker.getPersistentData().getInt(ConcussiveStrikeData.NBT_SOUND_DIRECT_TARGET_ID);
		if (soundTick == now && soundDirectTargetId == directTargetId)
			return;

		attacker.getPersistentData().putLong(ConcussiveStrikeData.NBT_SOUND_TICK, now);
		attacker.getPersistentData().putInt(ConcussiveStrikeData.NBT_SOUND_DIRECT_TARGET_ID, directTargetId);

		level.playSound(
				null,
				target.blockPosition(),
				TimothatysTrinketsModSounds.CONCUSSIVE_STRIKE.get(),
				SoundSource.PLAYERS,
				0.75F,
				0.9F + attacker.getRandom().nextFloat() * 0.12F
		);
	}

	private static void shakeCameraOnce(Player attacker) {
		if (!(attacker instanceof ServerPlayer serverPlayer))
			return;

		long now = attacker.level().getGameTime();
		int directTargetId = attacker.getPersistentData().getInt(ConcussiveStrikeData.NBT_LAST_DIRECT_TARGET_ID);
		long shakeTick = attacker.getPersistentData().getLong(ConcussiveStrikeData.NBT_SHAKE_TICK);
		int shakeDirectTargetId = attacker.getPersistentData().getInt(ConcussiveStrikeData.NBT_SHAKE_DIRECT_TARGET_ID);
		if (shakeTick == now && shakeDirectTargetId == directTargetId)
			return;

		attacker.getPersistentData().putLong(ConcussiveStrikeData.NBT_SHAKE_TICK, now);
		attacker.getPersistentData().putInt(ConcussiveStrikeData.NBT_SHAKE_DIRECT_TARGET_ID, directTargetId);
		PacketDistributor.sendToPlayer(serverPlayer, ConcussiveStrikeCameraShakeMessage.INSTANCE);
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
