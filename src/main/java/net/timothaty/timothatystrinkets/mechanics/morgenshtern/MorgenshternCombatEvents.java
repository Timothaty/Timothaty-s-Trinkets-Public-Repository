package net.timothaty.timothatystrinkets.mechanics.morgenshtern;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.morning_stars_judgment.MorningStarsJudgmentHandler;
import net.timothaty.timothatystrinkets.network.MorgenshternDecapitationMessage;
import net.timothaty.timothatystrinkets.network.MorgenshternStrikeMessage;
import net.timothaty.timothatystrinkets.util.StrikerOfTheMorningStarData;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class MorgenshternCombatEvents {
	private static final float FULL_CHARGE_THRESHOLD = 0.9F;
	private static final float HEAD_STRIKE_MOB_DAMAGE_MULTIPLIER = 2.0F;
	private static final float HEAD_STRIKE_PLAYER_DAMAGE_MULTIPLIER = 1.5F;
	private static final float NON_EXPLICIT_HEAD_DAMAGE_MULTIPLIER = 1.20F;
	private static final float HELMET_DURABILITY_CONVERSION = 0.50F;
	private static final float ARMOR_REDUCTION_REMAINING = 0.75F;
	private static final int EXTRA_ARMOR_DURABILITY_DAMAGE = 2;
	private static final int DECAPITATION_BLOOD_BITS = 34;
	private static final int HELMET_SPARK_COUNT = 9;
	private static final double SURVIVING_TARGET_RECOIL = 0.30D;
	private static final double HEAD_AIM_TOLERANCE = 0.06D;
	private static final double HEAD_ZONE_HEIGHT_RATIO = 0.11D;
	private static final double MIN_HEAD_ZONE_BELOW_EYES = 0.12D;
	private static final int STUN_CHAIN_REQUIRED_HITS = 4;
	private static final long STUN_CHAIN_MIN_INTERVAL_TICKS = 2L * 20L;
	private static final long STUN_CHAIN_MAX_INTERVAL_TICKS = 3L * 20L;
	private static final int STUN_CHAIN_DURATION_TICKS = 20;

	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD,
			EquipmentSlot.CHEST,
			EquipmentSlot.LEGS,
			EquipmentSlot.FEET
	};

	private static final Map<DamageSource, PendingHit> PENDING_HITS =
			new WeakHashMap<>();
	private static final Map<LivingEntity, FatalHeadStrike> FATAL_HEAD_STRIKES =
			new WeakHashMap<>();
	private static final Map<LivingEntity, StunHitChain> STUN_HIT_CHAINS =
			new WeakHashMap<>();

	private MorgenshternCombatEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingIncomingDamage(
			LivingIncomingDamageEvent event
	) {
		if (event == null || event.getAmount() <= 0.0F)
			return;

		LivingEntity target = event.getEntity();
		if (target == null || target.level().isClientSide())
			return;

		DamageSource source = event.getSource();
		if (!isDirectPlayerMeleeAttack(source))
			return;
		if (!(source.getEntity() instanceof ServerPlayer attacker))
			return;
		if (attacker.getPersistentData().getBoolean(
				StrikerOfTheMorningStarData.NBT_SHOCKWAVE_DAMAGE_GUARD
		))
			return;
		if (!attacker.getMainHandItem().is(
				TimothatysTrinketsModItems.MORGENSHTERN.get()
		))
			return;
		boolean fullyCharged = attacker.getAttackStrengthScale(0.5F)
				> FULL_CHARGE_THRESHOLD;
		if (!fullyCharged) {
			PENDING_HITS.put(
					source,
					new PendingHit(
							attacker,
							target,
							false,
							false,
							0.0F,
							null
					)
			);
			return;
		}

		event.addReductionModifier(
				DamageContainer.Reduction.ARMOR,
				(container, armorReduction) ->
						armorReduction * ARMOR_REDUCTION_REMAINING
		);

		boolean hasExplicitHead = target.getType().is(
				MorgenshternTags.EXPLICIT_HEADS
		);
		boolean headStrike = hasExplicitHead
				&& isAimingAtHead(attacker, target);
		HeadStrikeSound killSound = headStrike
				? classifyKillSound(target)
				: null;
		float ordinaryHitDamage = event.getAmount();
		float absorbedHeadStrikeDamage = 0.0F;
		if (headStrike) {
			float damageMultiplier = target instanceof Player
					? HEAD_STRIKE_PLAYER_DAMAGE_MULTIPLIER
					: HEAD_STRIKE_MOB_DAMAGE_MULTIPLIER;
			float headStrikeDamage = ordinaryHitDamage
					* damageMultiplier;
			if (hasProtectingHelmet(target)) {
				absorbedHeadStrikeDamage = Math.max(
						0.0F,
						headStrikeDamage - ordinaryHitDamage
				);
				headStrikeDamage -= absorbedHeadStrikeDamage;
			}
			event.setAmount(headStrikeDamage);
		} else if (!hasExplicitHead) {
			event.setAmount(
					ordinaryHitDamage * NON_EXPLICIT_HEAD_DAMAGE_MULTIPLIER
			);
		}

		PENDING_HITS.put(
				source,
				new PendingHit(
						attacker,
						target,
						true,
						headStrike,
						absorbedHeadStrikeDamage,
						killSound
				)
		);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamagePost(LivingDamageEvent.Post event) {
		PendingHit pending = PENDING_HITS.remove(event.getSource());
		if (pending == null || event.getNewDamage() <= 0.0F)
			return;

		MorningStarsJudgmentHandler.tryActivate(
				pending.attacker(),
				pending.target(),
				pending.fullyCharged(),
				pending.headStrike(),
				event.getNewDamage()
		);
		updateStunHitChain(pending.attacker(), pending.target());
		if (!pending.fullyCharged())
			return;

		damageWornArmor(pending.target());
		if (!pending.headStrike())
			return;

		if (pending.absorbedHeadStrikeDamage() > 0.0F) {
			damageProtectingHelmet(
					pending.target(),
					pending.absorbedHeadStrikeDamage()
			);
			spawnHelmetSparks(
					pending.attacker(),
					pending.target()
			);
		}

		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				pending.attacker(),
				new MorgenshternStrikeMessage(
						pending.attacker().getId(),
						pending.target().getId()
				)
		);

		if (pending.target().getHealth() <= 0.0F) {
			FATAL_HEAD_STRIKES.put(
					pending.target(),
					new FatalHeadStrike(
							event.getSource(),
							pending.killSound()
					)
			);
		} else {
			applySurvivingTargetRecoil(
					pending.attacker(),
					pending.target()
			);
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PENDING_HITS.clear();
		FATAL_HEAD_STRIKES.clear();
		STUN_HIT_CHAINS.clear();
	}

	@SubscribeEvent
	public static void onServerTickPost(ServerTickEvent.Post event) {
		Iterator<Map.Entry<LivingEntity, StunHitChain>> iterator =
				STUN_HIT_CHAINS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<LivingEntity, StunHitChain> entry = iterator.next();
			LivingEntity target = entry.getKey();
			StunHitChain chain = entry.getValue();
			if (target == null
					|| target.isRemoved()
					|| !target.isAlive()
					|| target.level().getGameTime()
							- chain.lastHitGameTime()
							> STUN_CHAIN_MAX_INTERVAL_TICKS) {
				iterator.remove();
			}
		}
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		if (event.getLevel().isClientSide())
			return;
		if (event.getEntity() instanceof LivingEntity living) {
			STUN_HIT_CHAINS.remove(living);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity victim = event.getEntity();
		if (victim == null || victim.level().isClientSide())
			return;

		STUN_HIT_CHAINS.remove(victim);
		FatalHeadStrike fatal = FATAL_HEAD_STRIKES.remove(victim);
		if (fatal == null
				|| fatal.source() != event.getSource()
				|| !victim.getType().is(MorgenshternTags.EXPLICIT_HEADS))
			return;

		PacketDistributor.sendToPlayersTrackingEntityAndSelf(
				victim,
				new MorgenshternDecapitationMessage(victim.getId())
		);
		playKillSound(victim, fatal.killSound());
		if (fatal.killSound() != HeadStrikeSound.SKELETON)
			spawnHeadBlood(victim);
	}

	private static HeadStrikeSound classifyKillSound(
			LivingEntity target
	) {
		if (target.getType().is(MorgenshternTags.SKELETONS))
			return HeadStrikeSound.SKELETON;
		if (target.getType().is(MorgenshternTags.PLATE_HEAD_ENTITIES))
			return HeadStrikeSound.PLATE;
		if (target.getType().is(
				MorgenshternTags.CHAINMAIL_HEAD_ENTITIES
		))
			return HeadStrikeSound.CHAINMAIL;

		ItemStack helmet = target.getItemBySlot(EquipmentSlot.HEAD);
		if (helmet.is(MorgenshternTags.LEATHER_HELMETS))
			return HeadStrikeSound.LEATHER;
		if (helmet.is(MorgenshternTags.CHAINMAIL_HELMETS))
			return HeadStrikeSound.CHAINMAIL;
		if (!helmet.isEmpty() && helmet.is(ItemTags.HEAD_ARMOR))
			return HeadStrikeSound.PLATE;
		return HeadStrikeSound.FLESH;
	}

	private static void playKillSound(
			LivingEntity victim,
			HeadStrikeSound killSound
	) {
		if (!(victim.level() instanceof ServerLevel serverLevel)
				|| killSound == null)
			return;

		float pitch = 1.0F
				+ serverLevel.getRandom().nextFloat() * 0.1F;
		serverLevel.playSound(
				null,
				victim.getX(),
				victim.getEyeY(),
				victim.getZ(),
				killSound.sound(),
				SoundSource.PLAYERS,
				1.0F,
				pitch
		);
	}

	private static boolean isDirectPlayerMeleeAttack(DamageSource source) {
		if (source == null || !source.is(DamageTypes.PLAYER_ATTACK))
			return false;
		Entity attacker = source.getEntity();
		return attacker instanceof ServerPlayer
				&& source.getDirectEntity() == attacker;
	}

	private static void updateStunHitChain(
			ServerPlayer attacker,
			LivingEntity target
	) {
		if (!target.isAlive() || target.isDeadOrDying()) {
			STUN_HIT_CHAINS.remove(target);
			return;
		}

		long now = target.level().getGameTime();
		UUID attackerId = attacker.getUUID();
		StunHitChain current = STUN_HIT_CHAINS.get(target);
		if (current == null || !current.attackerId().equals(attackerId)) {
			STUN_HIT_CHAINS.put(target, new StunHitChain(attackerId, 1, now));
			return;
		}

		long elapsed = now - current.lastHitGameTime();
		if (elapsed < STUN_CHAIN_MIN_INTERVAL_TICKS) {
			STUN_HIT_CHAINS.put(target, new StunHitChain(attackerId, 0, now));
			return;
		}
		if (elapsed > STUN_CHAIN_MAX_INTERVAL_TICKS) {
			STUN_HIT_CHAINS.put(target, new StunHitChain(attackerId, 1, now));
			return;
		}

		int hitCount = current.hitCount() + 1;
		if (hitCount < STUN_CHAIN_REQUIRED_HITS) {
			STUN_HIT_CHAINS.put(target, new StunHitChain(attackerId, hitCount, now));
			return;
		}

		STUN_HIT_CHAINS.remove(target);
		TimothatysTrinketsStunHelper.tryApplyStunSilently(
				target,
				attacker,
				STUN_CHAIN_DURATION_TICKS,
				STUN_CHAIN_DURATION_TICKS
		);
	}

	private static boolean isAimingAtHead(
			ServerPlayer attacker,
			LivingEntity target
	) {
		Vec3 rayStart = attacker.getEyePosition();
		AABB targetBounds = target.getBoundingBox();
		double rayLength = rayStart.distanceTo(targetBounds.getCenter())
				+ targetBounds.getSize() + 1.0D;
		Vec3 rayEnd = rayStart.add(
				attacker.getLookAngle().scale(rayLength)
		);

		AABB tolerantBounds = targetBounds.inflate(
				HEAD_AIM_TOLERANCE
		);
		return tolerantBounds.clip(rayStart, rayEnd)
				.map(hitPosition -> hitPosition.y >= headZoneBottom(
						target,
						targetBounds
				))
				.orElse(false);
	}

	private static double headZoneBottom(
			LivingEntity target,
			AABB targetBounds
	) {
		double belowEyes = Math.max(
				MIN_HEAD_ZONE_BELOW_EYES,
				targetBounds.getYsize() * HEAD_ZONE_HEIGHT_RATIO
		);
		return Math.max(
				targetBounds.minY + targetBounds.getYsize() * 0.55D,
				target.getEyeY() - belowEyes - HEAD_AIM_TOLERANCE
		);
	}

	private static void damageWornArmor(LivingEntity target) {
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack armor = target.getItemBySlot(slot);
			if (!armor.isEmpty() && armor.isDamageableItem()) {
				armor.hurtAndBreak(
						EXTRA_ARMOR_DURABILITY_DAMAGE,
						target,
						slot
				);
			}
		}
	}

	private static boolean hasProtectingHelmet(LivingEntity target) {
		ItemStack helmet = target.getItemBySlot(EquipmentSlot.HEAD);
		return !helmet.isEmpty()
				&& helmet.is(ItemTags.HEAD_ARMOR)
				&& !helmet.is(MorgenshternTags.HELMET_BLACKLIST);
	}

	private static void damageProtectingHelmet(
			LivingEntity target,
			float absorbedHeadStrikeDamage
	) {
		ItemStack helmet = target.getItemBySlot(EquipmentSlot.HEAD);
		if (helmet.isEmpty()
				|| !helmet.is(ItemTags.HEAD_ARMOR)
				|| helmet.is(MorgenshternTags.HELMET_BLACKLIST)
				|| !helmet.isDamageableItem())
			return;

		int durabilityDamage = Math.max(
				1,
				(int) Math.ceil(
						absorbedHeadStrikeDamage
								* HELMET_DURABILITY_CONVERSION
				)
		);
		helmet.hurtAndBreak(
				durabilityDamage,
				target,
				EquipmentSlot.HEAD
		);
	}

	private static void spawnHelmetSparks(
			ServerPlayer attacker,
			LivingEntity target
	) {
		if (!(target.level() instanceof ServerLevel serverLevel))
			return;

		double directionX = attacker.getX() - target.getX();
		double directionZ = attacker.getZ() - target.getZ();
		double horizontalLength = Math.sqrt(
				directionX * directionX + directionZ * directionZ
		);
		if (horizontalLength < 0.0001D) {
			directionX = 0.0D;
			directionZ = 1.0D;
			horizontalLength = 1.0D;
		}
		directionX /= horizontalLength;
		directionZ /= horizontalLength;

		double spawnOffset = Math.max(
				0.12D,
				target.getBbWidth() * 0.34D
		);
		double spawnX = target.getX() + directionX * spawnOffset;
		double spawnY = target.getEyeY() + 0.04D;
		double spawnZ = target.getZ() + directionZ * spawnOffset;

		for (int index = 0; index < HELMET_SPARK_COUNT; index++) {
			double sideways = (
					serverLevel.getRandom().nextDouble() - 0.5D
			) * 0.30D;
			double forward = 0.10D
					+ serverLevel.getRandom().nextDouble() * 0.14D;
			double velocityX =
					directionX * forward - directionZ * sideways;
			double velocityY = 0.07D
					+ serverLevel.getRandom().nextDouble() * 0.16D;
			double velocityZ =
					directionZ * forward + directionX * sideways;
			serverLevel.sendParticles(
					TimothatysTrinketsModParticleTypes.SPARK.get(),
					spawnX,
					spawnY,
					spawnZ,
					0,
					velocityX,
					velocityY,
					velocityZ,
					1.0D
			);
		}
	}

	private static void applySurvivingTargetRecoil(
			ServerPlayer attacker,
			LivingEntity target
	) {
		attacker.knockback(
				SURVIVING_TARGET_RECOIL,
				target.getX() - attacker.getX(),
				target.getZ() - attacker.getZ()
		);
	}

	private static void spawnHeadBlood(LivingEntity victim) {
		if (!(victim.level() instanceof ServerLevel serverLevel))
			return;

		double spread = Math.max(0.16D, victim.getBbWidth() * 0.28D);
		serverLevel.sendParticles(
				TimothatysTrinketsModParticleTypes.BLOOD_BIT.get(),
				victim.getX(),
				victim.getEyeY(),
				victim.getZ(),
				DECAPITATION_BLOOD_BITS,
				spread,
				0.16D,
				spread,
				0.12D
		);
	}

	private record PendingHit(
			ServerPlayer attacker,
			LivingEntity target,
			boolean fullyCharged,
			boolean headStrike,
			float absorbedHeadStrikeDamage,
			HeadStrikeSound killSound
	) {
	}

	private record StunHitChain(
			UUID attackerId,
			int hitCount,
			long lastHitGameTime
	) {
	}

	private record FatalHeadStrike(
			DamageSource source,
			HeadStrikeSound killSound
	) {
	}

	private enum HeadStrikeSound {
		LEATHER(TimothatysTrinketsModSounds.MACE_LEATHER_KILL.get()),
		CHAINMAIL(TimothatysTrinketsModSounds.MACE_CHAINMAIL_KILL.get()),
		PLATE(TimothatysTrinketsModSounds.MACE_PLATE_KILL.get()),
		FLESH(TimothatysTrinketsModSounds.MACE_FLESH_KILL.get()),
		SKELETON(TimothatysTrinketsModSounds.MACE_SKELETON_KILL.get());

		private final SoundEvent sound;

		HeadStrikeSound(SoundEvent sound) {
			this.sound = sound;
		}

		private SoundEvent sound() {
			return sound;
		}
	}
}
