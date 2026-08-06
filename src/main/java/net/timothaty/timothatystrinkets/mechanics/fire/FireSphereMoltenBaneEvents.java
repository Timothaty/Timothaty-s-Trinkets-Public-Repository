package net.timothaty.timothatystrinkets.mechanics.fire;

import org.joml.Vector3f;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsEffectSoundHandler;
import net.timothaty.timothatystrinkets.util.MoltenBaneMarkParticleData;
import net.timothaty.timothatystrinkets.util.TimothatysCuriosHelper;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class FireSphereMoltenBaneEvents {
	private static final double MOLTEN_BANE_CURSOR_RANGE = 5.0D;
	private static final float FULLY_CHARGED_ATTACK_THRESHOLD = 0.9F;
	private static final float MOLTEN_BANE_MAGIC_DAMAGE_CHANCE = 0.45F;
	private static final int MOLTEN_BANE_ENDING_WINDOW_TICKS = 5;
	private static final int UNDEAD_SLOWNESS_TICKS = 20 * 2;
	private static final int LAVAISH_STUN_TICKS = 20;
	private static final double LAVAISH_SPEED_REDUCTION = -0.20D;
	private static final float LAVAISH_DAMAGE_MULTIPLIER = 0.90F;
	private static final float METAL_MACE_DAMAGE_MULTIPLIER = 1.15F;
	private static final float METAL_PHYSICAL_DAMAGE_MULTIPLIER = 1.02F;

	private static final ResourceLocation LAVAISH_SPEED_REDUCTION_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "molten_bane_lavaish_speed_reduction");
	private static final ResourceLocation STUNNED_EFFECT_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "stunned");
	private static final TagKey<EntityType<?>> LAVAISH_MOBS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "lavaish_mobs"));
	private static final TagKey<EntityType<?>> METAL_GUYS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "metal_guys"));
	private static final DustParticleOptions YELLOW_SPARK = new DustParticleOptions(new Vector3f(1.0F, 0.86F, 0.12F), 0.75F);
	private static final DustParticleOptions ORANGE_SPARK = new DustParticleOptions(new Vector3f(1.0F, 0.36F, 0.02F), 0.75F);
	private static final Set<LivingEntity> PENDING_MOLTEN_BANE_ENDINGS =
			Collections.newSetFromMap(new WeakHashMap<>());

	private FireSphereMoltenBaneEvents() {
	}

	public static void tickMoltenBane(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel serverLevel))
			return;

		MobEffectInstance effect = entity.getEffect(TimothatysTrinketsModMobEffects.MOLTEN_BANE);
		if (effect == null) {
			cleanupLavaishDebuffs(entity);
			return;
		}

		boolean mechanicallyActive = !entity.isInWater();
		updateLavaishDebuffs(entity, mechanicallyActive && isLavaish(entity));
		if (!mechanicallyActive)
			return;

		if (effect.getDuration() <= MOLTEN_BANE_ENDING_WINDOW_TICKS) {
			scheduleMoltenBaneEnding(entity);
			return;
		}

		if (isMetalGuy(entity)) {
			spawnMetalSparks(serverLevel, entity);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onMoltenBaneRemoved(MobEffectEvent.Remove event) {
		if (event.isCanceled())
			return;
		if (!(event.getEntity() instanceof LivingEntity livingEntity))
			return;
		if (event.getEffect().value() != TimothatysTrinketsModMobEffects.MOLTEN_BANE.get())
			return;

		finishMoltenBane(livingEntity);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onMoltenBaneExpired(MobEffectEvent.Expired event) {
		if (event.isCanceled())
			return;
		MobEffectInstance effect = event.getEffectInstance();
		if (effect == null || effect.getEffect().value() != TimothatysTrinketsModMobEffects.MOLTEN_BANE.get())
			return;

		finishMoltenBane(event.getEntity());
	}

	@SubscribeEvent
	public static void onServerTickPost(ServerTickEvent.Post event) {
		Iterator<LivingEntity> iterator = PENDING_MOLTEN_BANE_ENDINGS.iterator();
		while (iterator.hasNext()) {
			LivingEntity entity = iterator.next();
			iterator.remove();
			if (entity == null || entity.isRemoved() || !(entity.level() instanceof ServerLevel serverLevel)) {
				continue;
			}
			MobEffectInstance current = entity.getEffect(TimothatysTrinketsModMobEffects.MOLTEN_BANE);
			if (current != null && current.getDuration() > MOLTEN_BANE_ENDING_WINDOW_TICKS) {
				continue;
			}
			performMoltenBaneEnding(serverLevel, entity);
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PENDING_MOLTEN_BANE_ENDINGS.clear();
	}

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event == null)
			return;

		LivingEntity target = event.getEntity();
		if (target == null || !target.isAlive())
			return;

		DamageSource source = event.getSource();
		if (source == null)
			return;

		applyOutgoingLavaishDamagePenalty(event, source);
		applyIncomingMetalPhysicalDamageBonus(event, source, target);

		Entity attackerEntity = source.getEntity();
		if (!(attackerEntity instanceof Player player))
			return;

		Level level = player.level();
		if (level.isClientSide())
			return;

		if (target == player)
			return;

		boolean pactProtectedTarget = PactOfAllianceHelper.areAllied(player, target);
		boolean directPlayerAttack = isDirectPlayerAttack(source, player);
		boolean fullyChargedAttack = directPlayerAttack && isFullyChargedAttack(player);
		if (isMoltenBaneMechanicallyActive(target) && fullyChargedAttack && !pactProtectedTarget) {
			spawnMoltenBaneHitFlames(player, target);
			tryApplyMoltenBaneMagicDamage(player, target);
			FireSphereSweepState.markNextSweepFiery(player);
		}

		if (!fullyChargedAttack)
			return;

		if (!hasFireSphereEquipped(player))
			return;

		if (pactProtectedTarget)
			return;

		if (player.getCooldowns().isOnCooldown(TimothatysTrinketsModItems.FIRE_SPHERE.get()))
			return;

		stackMoltenBaneMark(player, target);
	}

	private static void stackMoltenBaneMark(Player player, LivingEntity target) {
		if (PactOfAllianceHelper.areAllied(player, target))
			return;

		long now = player.level().getGameTime();
		CompoundTag targetData = target.getPersistentData();
		UUID playerUuid = player.getUUID();
		boolean sameOwner = targetData.hasUUID(FireSphereData.NBT_STACK_OWNER_UUID) && playerUuid.equals(targetData.getUUID(FireSphereData.NBT_STACK_OWNER_UUID));
		long expireTick = targetData.getLong(FireSphereData.NBT_EXPIRE_TICK);

		int stacks = 0;
		if (sameOwner && now <= expireTick) {
			stacks = targetData.getInt(FireSphereData.NBT_STACKS);
		}

		stacks = Math.min(FireSphereData.MAX_STACKS, stacks + 1);
		targetData.putUUID(FireSphereData.NBT_STACK_OWNER_UUID, playerUuid);
		targetData.putInt(FireSphereData.NBT_STACKS, stacks);
		targetData.putLong(FireSphereData.NBT_EXPIRE_TICK, now + FireSphereData.STACK_EXPIRE_TICKS);

		if (player.level() instanceof ServerLevel serverLevel) {
			spawnMoltenBaneMark(serverLevel, target, stacks);
		}

		if (stacks < FireSphereData.MAX_STACKS) {
			TimothatysTrinketsEffectSoundHandler.playMoltenBaneStackSound(target);
			return;
		}

		target.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.MOLTEN_BANE, FireSphereData.MOLTEN_BANE_DURATION_TICKS, 0, false, false, true), player);
		TimothatysTrinketsEffectSoundHandler.playMoltenBaneMarkSound(target);
		target.getPersistentData().remove(FireSphereData.NBT_MOLTEN_BANE_END_HANDLED);
		player.getPersistentData().putInt(FireSphereData.NBT_LAST_PROC_TARGET_ID, target.getId());
		player.getPersistentData().putLong(FireSphereData.NBT_LAST_PROC_EXPIRE_TICK, now + FireSphereData.MOLTEN_BANE_DURATION_TICKS);
		player.getCooldowns().addCooldown(TimothatysTrinketsModItems.FIRE_SPHERE.get(), FireSphereData.FIRE_SPHERE_COOLDOWN_TICKS);
		clearTargetStacks(target);
	}

	private static void tryApplyMoltenBaneMagicDamage(Player player, LivingEntity target) {
		float chance = MOLTEN_BANE_MAGIC_DAMAGE_CHANCE;
		if (player.getRandom().nextFloat() >= chance)
			return;
		applyMoltenBaneMagicDamage(player, target);
	}

	private static void applyMoltenBaneMagicDamage(Player player, LivingEntity target) {
		if (player == null || target == null || player.level().isClientSide())
			return;
		if (!player.isAlive() || !target.isAlive() || target.isRemoved())
			return;
		if (!isMoltenBaneMechanicallyActive(target))
			return;
		if (PactOfAllianceHelper.areAllied(player, target))
			return;

		DamageSource source = target.damageSources().indirectMagic(player, player);
		hurtWithoutKnockback(target, source, FireSphereData.MOLTEN_BANE_MAGIC_DAMAGE);
	}

	private static void hurtWithoutKnockback(LivingEntity entity, DamageSource source, float amount) {
		Vec3 movementBeforeDamage = entity.getDeltaMovement();
		int invulnerableTimeBeforeDamage = entity.invulnerableTime;

		entity.invulnerableTime = 0;
		boolean wasHurt = entity.hurt(source, amount);
		entity.invulnerableTime = Math.max(entity.invulnerableTime, invulnerableTimeBeforeDamage);

		if (wasHurt && entity.isAlive()) {
			entity.setDeltaMovement(movementBeforeDamage);
			entity.hurtMarked = false;
			entity.hasImpulse = false;
		}
	}

	private static void applyOutgoingLavaishDamagePenalty(LivingDamageEvent.Pre event, DamageSource source) {
		Entity attacker = source.getEntity();
		if (!(attacker instanceof LivingEntity livingAttacker))
			return;
		if (!isMoltenBaneMechanicallyActive(livingAttacker) || !isLavaish(livingAttacker))
			return;

		event.setNewDamage(event.getNewDamage() * LAVAISH_DAMAGE_MULTIPLIER);
	}

	private static void applyIncomingMetalPhysicalDamageBonus(LivingDamageEvent.Pre event, DamageSource source, LivingEntity target) {
		if (!isMoltenBaneMechanicallyActive(target) || !isMetalGuy(target))
			return;
		if (!isPhysicalDamage(source))
			return;

		float multiplier = isMaceHit(source) ? METAL_MACE_DAMAGE_MULTIPLIER : METAL_PHYSICAL_DAMAGE_MULTIPLIER;
		event.setNewDamage(event.getNewDamage() * multiplier);
	}

	private static void scheduleMoltenBaneEnding(LivingEntity entity) {
		if (entity == null || entity.level().isClientSide() || entity.isInWater())
			return;
		if (entity.getPersistentData().getBoolean(FireSphereData.NBT_MOLTEN_BANE_END_HANDLED))
			return;

		PENDING_MOLTEN_BANE_ENDINGS.add(entity);
	}

	private static void performMoltenBaneEnding(ServerLevel serverLevel, LivingEntity entity) {
		if (entity.isInWater())
			return;

		CompoundTag data = entity.getPersistentData();
		if (data.getBoolean(FireSphereData.NBT_MOLTEN_BANE_END_HANDLED))
			return;

		data.putBoolean(FireSphereData.NBT_MOLTEN_BANE_END_HANDLED, true);
		spawnMoltenBaneSmoke(serverLevel, entity);
		applyMoltenBaneEndMobInteractions(entity);
		if (!entity.hasEffect(TimothatysTrinketsModMobEffects.MOLTEN_BANE)) {
			data.remove(FireSphereData.NBT_MOLTEN_BANE_END_HANDLED);
		}
	}

	private static void finishMoltenBane(LivingEntity entity) {
		cleanupLavaishDebuffs(entity);
		if (entity.level() instanceof ServerLevel && !entity.isInWater()) {
			scheduleMoltenBaneEnding(entity);
		}

		entity.getPersistentData().remove(FireSphereData.NBT_MOLTEN_BANE_END_HANDLED);
		clearTargetStacks(entity);
	}

	private static void applyMoltenBaneEndMobInteractions(LivingEntity entity) {
		if (entity.isInWater())
			return;

		if (entity.getType().is(EntityTypeTags.UNDEAD)) {
			entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, UNDEAD_SLOWNESS_TICKS, 0, false, true, true));
		}

		if (isLavaish(entity)) {
			applyStunnedIfPresent(entity);
		}
	}

	private static void applyStunnedIfPresent(LivingEntity entity) {
		Optional<Holder.Reference<MobEffect>> stunned = BuiltInRegistries.MOB_EFFECT.getHolder(STUNNED_EFFECT_ID);
		stunned.ifPresent(effect -> entity.addEffect(new MobEffectInstance(effect, LAVAISH_STUN_TICKS, 0, false, false, true)));
	}

	private static void updateLavaishDebuffs(LivingEntity entity, boolean shouldApply) {
		updateAttributeModifier(entity, Attributes.MOVEMENT_SPEED, LAVAISH_SPEED_REDUCTION_ID, LAVAISH_SPEED_REDUCTION, shouldApply);
	}

	private static void cleanupLavaishDebuffs(LivingEntity entity) {
		removeAttributeModifier(entity, Attributes.MOVEMENT_SPEED, LAVAISH_SPEED_REDUCTION_ID);
	}

	private static void updateAttributeModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation modifierId, double amount, boolean shouldHaveModifier) {
		AttributeInstance attributeInstance = entity.getAttribute(attribute);
		if (attributeInstance == null)
			return;

		boolean hasModifier = attributeInstance.getModifier(modifierId) != null;
		if (shouldHaveModifier && !hasModifier) {
			attributeInstance.addTransientModifier(new AttributeModifier(modifierId, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		} else if (!shouldHaveModifier && hasModifier) {
			attributeInstance.removeModifier(modifierId);
		}
	}

	private static void removeAttributeModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation modifierId) {
		AttributeInstance attributeInstance = entity.getAttribute(attribute);
		if (attributeInstance != null && attributeInstance.getModifier(modifierId) != null) {
			attributeInstance.removeModifier(modifierId);
		}
	}

	private static boolean isMoltenBaneMechanicallyActive(LivingEntity entity) {
		return entity != null && entity.hasEffect(TimothatysTrinketsModMobEffects.MOLTEN_BANE) && !entity.isInWater();
	}

	private static boolean isLavaish(LivingEntity entity) {
		return entity.getType().is(LAVAISH_MOBS);
	}

	private static boolean isMetalGuy(LivingEntity entity) {
		return entity.getType().is(METAL_GUYS);
	}

	private static boolean isPhysicalDamage(DamageSource source) {
		return source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
	}

	private static boolean isMaceHit(DamageSource source) {
		return source.getEntity() instanceof Player player && player.getMainHandItem().is(Items.MACE);
	}

	private static boolean isFullyChargedAttack(Player player) {
		return player.getAttackStrengthScale(0.5F) >= FULLY_CHARGED_ATTACK_THRESHOLD;
	}

	private static boolean isDirectPlayerAttack(DamageSource source, Player player) {
		if (!source.is(DamageTypes.PLAYER_ATTACK))
			return false;
		return source.getEntity() == player && source.getDirectEntity() == player;
	}

	private static boolean hasFireSphereEquipped(Player player) {
		return TimothatysCuriosHelper.hasCurio(player, FireSphereData.FIRE_SPHERE_ID)
				|| player.getItemInHand(InteractionHand.MAIN_HAND).is(TimothatysTrinketsModItems.FIRE_SPHERE.get())
				|| player.getItemInHand(InteractionHand.OFF_HAND).is(TimothatysTrinketsModItems.FIRE_SPHERE.get());
	}

	private static void clearTargetStacks(LivingEntity target) {
		CompoundTag targetData = target.getPersistentData();
		targetData.remove(FireSphereData.NBT_TARGET_ID);
		targetData.remove(FireSphereData.NBT_STACK_OWNER_UUID);
		targetData.remove(FireSphereData.NBT_STACKS);
		targetData.remove(FireSphereData.NBT_EXPIRE_TICK);
	}

	private static void spawnMoltenBaneMark(ServerLevel serverLevel, LivingEntity target, int stage) {
		serverLevel.sendParticles(
				TimothatysTrinketsModParticleTypes.MOLTEN_BANE_MARK.get(),
				target.getX(),
				target.getY() + target.getBbHeight(),
				target.getZ(),
				0,
				MoltenBaneMarkParticleData.encodeEntityId(target),
				MoltenBaneMarkParticleData.attachMagic(),
				MoltenBaneMarkParticleData.encodeStage(stage),
				1.0D
		);
	}

	private static void spawnMoltenBaneHitFlames(Player player, LivingEntity target) {
		if (!(player.level() instanceof ServerLevel serverLevel))
			return;

		Vec3 pos = findLookedAtPointOnTarget(player, target, MOLTEN_BANE_CURSOR_RANGE);
		serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 8, 0.08D, 0.08D, 0.08D, 0.01D);
	}

	private static void spawnMetalSparks(ServerLevel serverLevel, LivingEntity target) {
		double y = target.getY() + target.getBbHeight() * (0.25D + target.getRandom().nextDouble() * 0.55D);
		double x = target.getX() + (target.getRandom().nextDouble() - 0.5D) * target.getBbWidth();
		double z = target.getZ() + (target.getRandom().nextDouble() - 0.5D) * target.getBbWidth();
		serverLevel.sendParticles(YELLOW_SPARK, x, y, z, 2, 0.05D, 0.05D, 0.05D, 0.02D);
		serverLevel.sendParticles(ORANGE_SPARK, x, y, z, 2, 0.05D, 0.05D, 0.05D, 0.02D);
	}

	private static void spawnMoltenBaneSmoke(ServerLevel serverLevel, LivingEntity target) {
		serverLevel.sendParticles(
				ParticleTypes.SMOKE,
				target.getX(),
				target.getY() + target.getBbHeight() * 0.55D,
				target.getZ(),
				32,
				target.getBbWidth() * 0.45D,
				target.getBbHeight() * 0.32D,
				target.getBbWidth() * 0.45D,
				0.045D
		);
	}

	private static Vec3 findLookedAtPointOnTarget(Player player, LivingEntity target, double range) {
		Vec3 eye = player.getEyePosition(1.0F);
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = eye.add(look.scale(range));
		Level level = player.level();

		double maxDistanceSqr = range * range;
		BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (blockHit.getType() != HitResult.Type.MISS) {
			maxDistanceSqr = eye.distanceToSqr(blockHit.getLocation());
		}

		AABB hitBox = target.getBoundingBox().inflate(target.getPickRadius() + 0.35D);
		Optional<Vec3> hit = hitBox.clip(eye, end);
		if (hit.isPresent() && eye.distanceToSqr(hit.get()) <= maxDistanceSqr) {
			return hit.get();
		}

		HitResult pick = player.pick(range, 1.0F, false);
		if (pick.getType() != HitResult.Type.MISS) {
			return pick.getLocation();
		}

		return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
	}
}
