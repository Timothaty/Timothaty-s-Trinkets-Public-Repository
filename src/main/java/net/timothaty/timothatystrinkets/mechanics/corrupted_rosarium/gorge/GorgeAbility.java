package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.gorge;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumCombination;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumData;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumHelper;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumState;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.CorruptedRosariumTargeting;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;

public final class GorgeAbility {
	public static final TagKey<EntityType<?>> BLACKLIST = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "gorge_blacklist")
	);

	private GorgeAbility() {
	}

	public static boolean tryActivate(Player player) {
		return player instanceof ServerPlayer serverPlayer && tryActivate(serverPlayer);
	}

	public static boolean tryActivate(ServerPlayer player) {
		if (player == null || !player.isAlive() || player.isSpectator())
			return false;
		if (player.getCooldowns().isOnCooldown(TimothatysTrinketsModItems.CORRUPTED_ROSARY.get()))
			return false;
		if (GorgeState.hasActiveSession(player)
				|| player.hasEffect(TimothatysTrinketsModMobEffects.GORGE))
			return false;
		SlotResult sourceRosarium = findActiveGorgeRosarium(player).orElse(null);
		if (sourceRosarium == null)
			return false;

		Animal target = findLookedAtTarget(player);
		if (target == null)
			return false;

		float targetCurrentHealth = target.getHealth();
		GorgeData.Restoration restoration = GorgeData.calculateRestoration(targetCurrentHealth);
		if (!GorgeState.begin(
				player,
				restoration,
				CorruptedRosariumState.getRevision(player),
				sourceRosarium.slotContext()
		))
			return false;

		MobEffectInstance gorge = new MobEffectInstance(
				TimothatysTrinketsModMobEffects.GORGE,
				GorgeData.DURATION_TICKS,
				0,
				false,
				false,
				true
		);
		if (!player.addEffect(gorge, player)) {
			GorgeState.cancelWithoutPenalty(player, false);
			return false;
		}
		if (player.isUsingItem()
				&& player.getUseItem().getFoodProperties(player) != null) {
			player.stopUsingItem();
		}

		GorgeVisuals.ConsumptionSnapshot visualSnapshot =
				GorgeVisuals.capture(target);
		boolean dropSaddle = target instanceof Saddleable saddleable
				&& saddleable.isSaddled();
		GorgeVisuals.emitSuccessfulConsumption(player, visualSnapshot);
		if (dropSaddle)
			target.spawnAtLocation(new ItemStack(Items.SADDLE));
		target.discard();
		GorgeData.addHungerWithoutSaturation(player, GorgeData.IMMEDIATE_HUNGER);
		player.getCooldowns().addCooldown(
				TimothatysTrinketsModItems.CORRUPTED_ROSARY.get(),
				GorgeData.COOLDOWN_TICKS
		);
		return true;
	}

	private static Optional<SlotResult> findActiveGorgeRosarium(ServerPlayer player) {
		CorruptedRosariumState.markDirty(player);
		CorruptedRosariumState.refreshNow(player);
		return CorruptedRosariumHelper.findActiveRosariumResult(player)
				.filter(result -> CorruptedRosariumData.getCombination(result.stack())
						.filter(combination -> combination == CorruptedRosariumCombination.GORGE)
						.isPresent());
	}

	private static Animal findLookedAtTarget(ServerPlayer player) {
		Vec3 eye = player.getEyePosition(1.0F);
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = eye.add(look.scale(GorgeData.RAY_TRACE_DISTANCE));
		Level level = player.level();

		double obstructionDistanceSqr = GorgeData.RAY_TRACE_DISTANCE * GorgeData.RAY_TRACE_DISTANCE;
		BlockHitResult blockHit = level.clip(
				new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)
		);
		if (blockHit.getType() != HitResult.Type.MISS)
			obstructionDistanceSqr = eye.distanceToSqr(blockHit.getLocation());

		AABB searchBox = player.getBoundingBox()
				.expandTowards(look.scale(GorgeData.RAY_TRACE_DISTANCE))
				.inflate(1.0D);
		Entity firstHit = null;
		double firstHitDistanceSqr = obstructionDistanceSqr;

		for (Entity candidate : level.getEntities(
				player,
				searchBox,
				entity -> !entity.isSpectator() && entity.isPickable()
		)) {
			AABB hitBox = candidate.getBoundingBox().inflate(candidate.getPickRadius());
			Optional<Vec3> hit = hitBox.contains(eye) ? Optional.of(eye) : hitBox.clip(eye, end);
			if (hit.isEmpty())
				continue;

			double hitDistanceSqr = eye.distanceToSqr(hit.get());
			if (hitDistanceSqr <= firstHitDistanceSqr) {
				firstHit = candidate;
				firstHitDistanceSqr = hitDistanceSqr;
			}
		}

		if (!(firstHit instanceof Animal animal) || !isValidTarget(player, animal, eye))
			return null;
		return animal;
	}

	private static boolean isValidTarget(ServerPlayer player, Animal animal, Vec3 eye) {
		if (!animal.isAlive() || animal.isBaby() || animal.getMaxHealth() > GorgeData.MAX_TARGET_HEALTH)
			return false;
		if (animal instanceof Enemy || isTamed(animal))
			return false;
		if (animal.getType().is(BLACKLIST))
			return false;
		if (CorruptedRosariumTargeting.isProtectedCombatTarget(player, animal))
			return false;
		if (distanceToBoundingBoxSqr(eye, animal.getBoundingBox()) > GorgeData.MAX_TARGET_REACH_SQR)
			return false;
		return player.hasLineOfSight(animal);
	}

	private static boolean isTamed(Animal animal) {
		if (animal instanceof TamableAnimal tamable && tamable.isTame())
			return true;
		return animal instanceof AbstractHorse horse && horse.isTamed();
	}

	private static double distanceToBoundingBoxSqr(Vec3 point, AABB box) {
		double dx = Math.max(Math.max(box.minX - point.x, 0.0D), point.x - box.maxX);
		double dy = Math.max(Math.max(box.minY - point.y, 0.0D), point.y - box.maxY);
		double dz = Math.max(Math.max(box.minZ - point.z, 0.0D), point.z - box.maxZ);
		return dx * dx + dy * dy + dz * dz;
	}
}
