package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public class PurgeEffectHandler {
	private static final String NBT_PURGE_CURE_GUARD = "ttr_purge_cure_guard";
	private static final String NBT_PURGE_CURE_GUARD_UNTIL = "ttr_purge_cure_guard_until";

	private static final float UNDEAD_REFLECT_MULTIPLIER = 0.65F;
	private static final long CURE_GUARD_TICKS = 120L;
	private static final TagKey<MobEffect> DEBUFF_EXCEPTIONS_TAG = TagKey.create(
			Registries.MOB_EFFECT,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "debuff_exceptions")
	);
	private static final Set<LivingEntity> ACTIVE_PURGE_ENTITIES =
			Collections.newSetFromMap(new WeakHashMap<>());

	private static Holder<MobEffect> purge() {
		return TimothatysTrinketsModMobEffects.PURGE;
	}

	public static boolean hasPurge(LivingEntity entity) {
		return entity != null && entity.hasEffect(purge());
	}

	public static void clearHarmfulEffects(LivingEntity entity) {
		if (entity == null) {
			return;
		}

		boolean hasRemovableEffect = false;
		for (MobEffectInstance instance : entity.getActiveEffects()) {
			if (instance.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
				continue;
			}
			if (instance.getEffect().is(DEBUFF_EXCEPTIONS_TAG)) {
				continue;
			}
			hasRemovableEffect = true;
			break;
		}
		if (!hasRemovableEffect) {
			return;
		}

		boolean removedAny = false;
		for (MobEffectInstance instance : new ArrayList<>(entity.getActiveEffects())) {
			if (instance.getEffect().value().getCategory() != MobEffectCategory.HARMFUL
					|| instance.getEffect().is(DEBUFF_EXCEPTIONS_TAG)) {
				continue;
			}
			if (entity.removeEffect(instance.getEffect())) {
				removedAny = true;
			}
		}

		if (removedAny) {
			playPurgeCleanseFx(entity);
		}
	}

	private static void playPurgeCleanseFx(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		DustParticleOptions dust = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 0.694F), 1.2F);
		BlockPos pos = entity.blockPosition();
		serverLevel.sendParticles(
				dust,
				entity.getX(), entity.getY() + 1.0D, entity.getZ(),
				20,
				0.35D, 0.45D, 0.35D,
				0.01D
		);
		serverLevel.playSound(
				null,
				pos,
				SoundEvents.AMETHYST_BLOCK_CHIME,
				SoundSource.PLAYERS,
				0.85F,
				1.35F
		);
	}

	@SubscribeEvent
	public static void onCureItemStart(LivingEntityUseItemEvent.Start event) {
		LivingEntity living = event.getEntity();
		if (living == null || !hasPurge(living)) {
			return;
		}
		if (!(living.level() instanceof Level level) || level.isClientSide) {
			return;
		}
		if (!event.getItem().is(Items.MILK_BUCKET) && !event.getItem().is(Items.HONEY_BOTTLE)) {
			return;
		}

		var data = living.getPersistentData();
		data.putBoolean(NBT_PURGE_CURE_GUARD, true);
		data.putLong(NBT_PURGE_CURE_GUARD_UNTIL, level.getGameTime() + CURE_GUARD_TICKS);
	}

	@SubscribeEvent
	public static void onCureItemStop(LivingEntityUseItemEvent.Stop event) {
		if (event.getEntity() == null) {
			return;
		}
		if (event.getItem().is(Items.MILK_BUCKET) || event.getItem().is(Items.HONEY_BOTTLE)) {
			clearCureGuard(event.getEntity());
		}
	}

	@SubscribeEvent
	public static void onCureItemFinish(LivingEntityUseItemEvent.Finish event) {
		if (event.getEntity() == null) {
			return;
		}
		if (event.getItem().is(Items.MILK_BUCKET) || event.getItem().is(Items.HONEY_BOTTLE)) {
			clearCureGuard(event.getEntity());
		}
	}

	@SubscribeEvent
	public static void onEffectAdded(MobEffectEvent.Added event) {
		if (!event.getEntity().level().isClientSide() && isPurge(event.getEffectInstance())) {
			ACTIVE_PURGE_ENTITIES.add(event.getEntity());
		}
	}

	@SubscribeEvent
	public static void onEffectRemove(MobEffectEvent.Remove event) {
		LivingEntity living = event.getEntity();
		if (living == null || !hasPurge(living)) {
			return;
		}
		if (!(living.level() instanceof Level level) || level.isClientSide) {
			return;
		}

		var data = living.getPersistentData();
		if (!data.getBoolean(NBT_PURGE_CURE_GUARD)) {
			return;
		}

		long now = level.getGameTime();
		if (now > data.getLong(NBT_PURGE_CURE_GUARD_UNTIL)) {
			clearCureGuard(living);
			return;
		}

		MobEffectInstance instance = event.getEffectInstance();
		if (instance != null && instance.is(purge())) {
			event.setCanceled(true);
		}
	}

	public static void tickPurge(LivingEntity living) {
		Level level = living.level();
		if (level.isClientSide) {
			return;
		}

		var data = living.getPersistentData();
		if (data.getBoolean(NBT_PURGE_CURE_GUARD) && level.getGameTime() > data.getLong(NBT_PURGE_CURE_GUARD_UNTIL)) {
			clearCureGuard(living);
		}

		clearHarmfulEffects(living);
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
			return;
		}

		if (hasPurge(living)) {
			ACTIVE_PURGE_ENTITIES.add(living);
		}

		var data = living.getPersistentData();
		if (data.getBoolean(NBT_PURGE_CURE_GUARD)
				&& (!hasPurge(living) || living.level().getGameTime() > data.getLong(NBT_PURGE_CURE_GUARD_UNTIL))) {
			clearCureGuard(living);
		}
	}

	@SubscribeEvent
	public static void onServerTickPre(ServerTickEvent.Pre event) {
		if (ACTIVE_PURGE_ENTITIES.isEmpty()) {
			return;
		}

		for (LivingEntity living : new ArrayList<>(ACTIVE_PURGE_ENTITIES)) {
			if (living == null || living.isRemoved() || !living.isAlive() || living.level().isClientSide() || !hasPurge(living)) {
				ACTIVE_PURGE_ENTITIES.remove(living);
				continue;
			}
			tickPurge(living);
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ACTIVE_PURGE_ENTITIES.clear();
	}

	@SubscribeEvent
	public static void onLivingDamagePost(LivingDamageEvent.Post event) {
		LivingEntity victim = event.getEntity();
		if (victim == null || victim.level().isClientSide || !hasPurge(victim)) {
			return;
		}

		float takenDamage = event.getNewDamage();
		if (takenDamage <= 0.0F) {
			return;
		}

		DamageSource source = event.getSource();
		if (source == null || source.is(DamageTypes.THORNS)) {
			return;
		}

		Entity attackerEntity = source.getEntity();
		if (!(attackerEntity instanceof LivingEntity attacker) || !attacker.isInvertedHealAndHarm()) {
			return;
		}

		float reflectedDamage = takenDamage * UNDEAD_REFLECT_MULTIPLIER;
		if (reflectedDamage <= 0.0F || !attacker.isAlive()) {
			return;
		}

		attacker.hurt(victim.damageSources().thorns(victim), reflectedDamage);
	}

	private static void clearCureGuard(LivingEntity living) {
		var data = living.getPersistentData();
		data.remove(NBT_PURGE_CURE_GUARD);
		data.remove(NBT_PURGE_CURE_GUARD_UNTIL);
	}

	private static boolean isPurge(MobEffectInstance instance) {
		return instance != null && instance.is(purge());
	}
}
