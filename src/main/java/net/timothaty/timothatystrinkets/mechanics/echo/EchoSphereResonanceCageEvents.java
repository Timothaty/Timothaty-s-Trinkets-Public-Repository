package net.timothaty.timothatystrinkets.mechanics.echo;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsEquipState;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Set;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class EchoSphereResonanceCageEvents {
	private static final Set<LivingEntity> ACTIVE_CAGES =
			Collections.newSetFromMap(new WeakHashMap<>());

	private EchoSphereResonanceCageEvents() {
	}


	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (event == null) return;

		LivingEntity target = event.getEntity();
		if (target == null || !target.isAlive()) return;

		if (isResonanceCaged(target)) {
			event.setCanceled(true);
			return;
		}

		DamageSource source = event.getSource();
		if (source == null) return;

		Entity attackerEntity = source.getEntity();
		if (attackerEntity instanceof LivingEntity livingAttacker && isResonanceCaged(livingAttacker)) {
			event.setCanceled(true);
			return;
		}

		Entity directEntity = source.getDirectEntity();
		if (directEntity instanceof LivingEntity directLiving && isResonanceCaged(directLiving)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event == null) return;

		LivingEntity target = event.getEntity();
		if (target == null || !target.isAlive()) return;

		if (isResonanceCaged(target)) {
			event.setNewDamage(0.0F);
			return;
		}

		DamageSource source = event.getSource();
		if (source == null) return;

		Entity attackerEntity = source.getEntity();
		if (attackerEntity instanceof LivingEntity livingAttacker && isResonanceCaged(livingAttacker)) {
			event.setNewDamage(0.0F);
			return;
		}

		Entity directEntity = source.getDirectEntity();
		if (directEntity instanceof LivingEntity directLiving && isResonanceCaged(directLiving)) {
			event.setNewDamage(0.0F);
			return;
		}

		if (!(attackerEntity instanceof Player player)) return;
		if (player.level().isClientSide()) return;
		if (target == player || target instanceof Player playerTarget && (playerTarget.isCreative() || playerTarget.isSpectator())) return;
		if (PactOfAllianceHelper.areAllied(player, target)) return;
		if (TimothatysTrinketsStunHelper.isStunned(target) || TimothatysTrinketsStunHelper.isStaggered(target)) return;
		if (!TimothatysTrinketsEquipState.has(player, TimothatysTrinketsEquipState.ECHO_SPHERE)) return;
		if (player.getCooldowns().isOnCooldown(TimothatysTrinketsModItems.ECHO_SPHERE.get())) return;
		if (isLethalDamage(target, event.getNewDamage())) return;

		applyResonanceCage(target, player);
		player.getCooldowns().addCooldown(TimothatysTrinketsModItems.ECHO_SPHERE.get(), EchoSphereData.ECHO_SPHERE_COOLDOWN_TICKS);
	}

	@SubscribeEvent
	public static void onResonanceCageRemove(MobEffectEvent.Remove event) {
		LivingEntity target = event.getEntity();
		if (target == null || target.level().isClientSide()) return;

		MobEffectInstance instance = event.getEffectInstance();
		if (instance != null && instance.is(TimothatysTrinketsModMobEffects.RESONANCE_CAGE) && isResonanceCaged(target)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onResonanceCageAdded(MobEffectEvent.Added event) {
		LivingEntity target = event.getEntity();
		MobEffectInstance instance = event.getEffectInstance();
		if (!target.level().isClientSide()
				&& instance != null
				&& instance.is(TimothatysTrinketsModMobEffects.RESONANCE_CAGE)
				&& target.getPersistentData().getLong(EchoSphereData.NBT_UNTIL) > 0L) {
			ACTIVE_CAGES.add(target);
		}
	}

	@SubscribeEvent
	public static void onServerTickPre(ServerTickEvent.Pre event) {
		if (ACTIVE_CAGES.isEmpty()) {
			return;
		}

		for (LivingEntity target : new ArrayList<>(ACTIVE_CAGES)) {
			if (target == null || target.isRemoved() || target.level().isClientSide()) {
				ACTIVE_CAGES.remove(target);
				continue;
			}

			CompoundTag tag = target.getPersistentData();
			long until = tag.getLong(EchoSphereData.NBT_UNTIL);
			if (until <= 0L) {
				ACTIVE_CAGES.remove(target);
				continue;
			}

			long now = target.level().getGameTime();
			if (now >= until) {
				ACTIVE_CAGES.remove(target);
				releaseTarget(target);
				continue;
			}

			tickActiveResonanceCage(target, until, now);
		}
	}

	private static void tickActiveResonanceCage(LivingEntity target, long until, long now) {
		Level level = target.level();

		CompoundTag tag = target.getPersistentData();
		boolean deepDark = tag.getBoolean(EchoSphereData.NBT_DEEP_DARK);
		freezeTarget(target);
		keepResonanceCageState(target, (int) Math.min(Integer.MAX_VALUE, until - now));
		ResonanceCageInterrupts.keepInterrupted(target);

		if (level instanceof ServerLevel server && target.tickCount % 2 == 0) {
			EchoSphereResonanceCageVisuals.spawnLoop(server, target, deepDark);
		}
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity target)) {
			return;
		}

		long until = target.getPersistentData().getLong(EchoSphereData.NBT_UNTIL);
		if (until <= 0L) {
			return;
		}

		long now = target.level().getGameTime();
		if (now >= until) {
			releaseTarget(target);
		} else {
			ACTIVE_CAGES.add(target);
			keepResonanceCageState(target, (int) Math.min(Integer.MAX_VALUE, until - now));
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ACTIVE_CAGES.clear();
	}

	private static void applyResonanceCage(LivingEntity target, Player caster) {
		if (TimothatysTrinketsStunHelper.isStunned(target) || TimothatysTrinketsStunHelper.isStaggered(target)) return;

		CompoundTag tag = target.getPersistentData();
		long now = target.level().getGameTime();
		boolean alreadyCaged = isResonanceCaged(target);
		boolean deepDark = isInDeepDark(target);

		if (!alreadyCaged) {
			tag.putDouble(EchoSphereData.NBT_X, target.getX());
			tag.putDouble(EchoSphereData.NBT_Y, target.getY());
			tag.putDouble(EchoSphereData.NBT_Z, target.getZ());
			tag.putFloat(EchoSphereData.NBT_YROT, target.getYRot());
			tag.putFloat(EchoSphereData.NBT_XROT, target.getXRot());
			tag.putLong(EchoSphereData.NBT_HIDE_AT, now + EchoSphereData.MODEL_HIDE_DELAY_TICKS);
			tag.putBoolean(EchoSphereData.NBT_OLD_INVISIBLE, target.isInvisible());
			tag.putBoolean(EchoSphereData.NBT_OLD_INVULNERABLE, target.isInvulnerable());
			tag.putBoolean(EchoSphereData.NBT_OLD_NO_GRAVITY, target.isNoGravity());
			tag.putBoolean(EchoSphereData.NBT_OLD_NO_PHYSICS, target.noPhysics);

			if (target instanceof Mob mob) {
				tag.putBoolean(EchoSphereData.NBT_OLD_NO_AI, mob.isNoAi());
				mob.setNoAi(true);
			}
		}

		tag.putLong(EchoSphereData.NBT_UNTIL, now + EchoSphereData.CAGE_DURATION_TICKS);
		tag.putUUID(EchoSphereData.NBT_CASTER_UUID, caster.getUUID());
		tag.putBoolean(EchoSphereData.NBT_DEEP_DARK, deepDark);
		ACTIVE_CAGES.add(target);

		ResonanceCageInterrupts.interrupt(target);
		target.stopRiding();
		target.setDeltaMovement(Vec3.ZERO);
		target.fallDistance = 0.0F;
		keepResonanceCageState(target, EchoSphereData.CAGE_DURATION_TICKS + 5);

		if (target.level() instanceof ServerLevel server) {
			EchoSphereResonanceCageVisuals.spawnEnter(server, target, EchoSphereData.CAGE_DURATION_TICKS, deepDark);
		}
	}

	private static void keepResonanceCageState(LivingEntity target, int remainingTicks) {
		CompoundTag tag = target.getPersistentData();
		long hideAt = tag.getLong(EchoSphereData.NBT_HIDE_AT);
		boolean shouldHideModel = hideAt <= 0L || target.level().getGameTime() >= hideAt;

		target.setInvisible(shouldHideModel || tag.getBoolean(EchoSphereData.NBT_OLD_INVISIBLE));
		target.setInvulnerable(true);
		target.setNoGravity(true);
		target.noPhysics = true;
		makeIntangible(target);
		if (!target.hasEffect(TimothatysTrinketsModMobEffects.RESONANCE_CAGE)) {
			target.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.RESONANCE_CAGE, Math.max(remainingTicks, 5), 0, false, true, true));
		}
	}

	private static void freezeTarget(LivingEntity target) {
		CompoundTag tag = target.getPersistentData();
		double x = tag.getDouble(EchoSphereData.NBT_X);
		double y = tag.getDouble(EchoSphereData.NBT_Y);
		double z = tag.getDouble(EchoSphereData.NBT_Z);
		float yRot = tag.getFloat(EchoSphereData.NBT_YROT);
		float xRot = tag.getFloat(EchoSphereData.NBT_XROT);

		target.setDeltaMovement(Vec3.ZERO);
		target.fallDistance = 0.0F;
		target.teleportTo(x, y, z);
		target.setYRot(yRot);
		target.setXRot(xRot);
	}

	private static void makeIntangible(LivingEntity target) {
		double x = target.getX();
		double y = target.getY();
		double z = target.getZ();
		target.setBoundingBox(new AABB(x - EchoSphereData.INTANGIBLE_BOX_HALF_SIZE, y, z - EchoSphereData.INTANGIBLE_BOX_HALF_SIZE, x + EchoSphereData.INTANGIBLE_BOX_HALF_SIZE, y + EchoSphereData.INTANGIBLE_BOX_HALF_SIZE, z + EchoSphereData.INTANGIBLE_BOX_HALF_SIZE));
	}

	private static void releaseTarget(LivingEntity target) {
		CompoundTag tag = target.getPersistentData();
		boolean deepDark = tag.getBoolean(EchoSphereData.NBT_DEEP_DARK);
		Entity caster = null;

		if (target.level() instanceof ServerLevel server && tag.hasUUID(EchoSphereData.NBT_CASTER_UUID)) {
			caster = server.getEntity(tag.getUUID(EchoSphereData.NBT_CASTER_UUID));
		}

		target.setInvisible(tag.getBoolean(EchoSphereData.NBT_OLD_INVISIBLE));
		target.setInvulnerable(tag.getBoolean(EchoSphereData.NBT_OLD_INVULNERABLE));
		target.setNoGravity(tag.getBoolean(EchoSphereData.NBT_OLD_NO_GRAVITY));
		target.noPhysics = tag.getBoolean(EchoSphereData.NBT_OLD_NO_PHYSICS);
		target.refreshDimensions();
		if (target instanceof Mob mob) {
			mob.setNoAi(tag.getBoolean(EchoSphereData.NBT_OLD_NO_AI));
		}

		clearResonanceCageTags(tag);
		target.removeEffect(TimothatysTrinketsModMobEffects.RESONANCE_CAGE);

		if (target.level() instanceof ServerLevel server) {
			EchoSphereResonanceCageVisuals.spawnRelease(server, target, deepDark);
		}

		if (!target.isAlive()) return;

		Entity damageOwner = caster != null ? caster : target;
		if (caster instanceof Player playerCaster && PactOfAllianceHelper.areAllied(playerCaster, target)) return;
		float damage = deepDark ? EchoSphereData.RELEASE_DAMAGE * EchoSphereData.DEEP_DARK_RELEASE_DAMAGE_MULTIPLIER : EchoSphereData.RELEASE_DAMAGE;
		target.invulnerableTime = 0;
		target.hurt(target.damageSources().indirectMagic(damageOwner, damageOwner), damage);

		if (!target.isAlive()) {
			if (target.level() instanceof ServerLevel server) {
				EchoSphereResonanceCageVisuals.spawnDeathSouls(server, target, deepDark);
			}
			if (caster instanceof LivingEntity livingCaster && livingCaster.isAlive()) {
				livingCaster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EchoSphereData.KILL_REGENERATION_TICKS, 0, false, true, true));
			}
		}
	}

	public static boolean isResonanceCaged(LivingEntity entity) {
		if (entity == null) return false;
		long until = entity.getPersistentData().getLong(EchoSphereData.NBT_UNTIL);
		return until > entity.level().getGameTime();
	}

	private static boolean isLethalDamage(LivingEntity target, float damage) {
		return damage > 0.0F && target.getHealth() <= damage;
	}

	private static boolean isInDeepDark(LivingEntity entity) {
		return entity.level().getBiome(entity.blockPosition()).is(EchoSphereData.DEEP_DARK_BIOME);
	}

	private static void clearResonanceCageTags(CompoundTag tag) {
		tag.remove(EchoSphereData.NBT_UNTIL);
		tag.remove(EchoSphereData.NBT_X);
		tag.remove(EchoSphereData.NBT_Y);
		tag.remove(EchoSphereData.NBT_Z);
		tag.remove(EchoSphereData.NBT_YROT);
		tag.remove(EchoSphereData.NBT_XROT);
		tag.remove(EchoSphereData.NBT_HIDE_AT);
		tag.remove(EchoSphereData.NBT_OLD_INVISIBLE);
		tag.remove(EchoSphereData.NBT_OLD_INVULNERABLE);
		tag.remove(EchoSphereData.NBT_OLD_NO_GRAVITY);
		tag.remove(EchoSphereData.NBT_OLD_NO_PHYSICS);
		tag.remove(EchoSphereData.NBT_OLD_NO_AI);
		tag.remove(EchoSphereData.NBT_DEEP_DARK);
		tag.remove(EchoSphereData.NBT_CASTER_UUID);
	}
}
