package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.VillagerPanicTrigger;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.WeakHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerPanicTrigger.class)
public abstract class VillagerPanicTriggerNecromancyMixin {
	@Unique
	private static final double TIMOTHATYS_TRINKETS$NECROMANCY_PANIC_RADIUS = 10.0D;
	@Unique
	private static final double TIMOTHATYS_TRINKETS$NECROMANCY_PANIC_RADIUS_SQR =
		TIMOTHATYS_TRINKETS$NECROMANCY_PANIC_RADIUS * TIMOTHATYS_TRINKETS$NECROMANCY_PANIC_RADIUS;
	@Unique
	private static final long TIMOTHATYS_TRINKETS$NECROMANCY_PANIC_MEMORY_TICKS = 100L;
	@Unique
	private static final long TIMOTHATYS_TRINKETS$NEGATIVE_SCAN_INTERVAL_TICKS = 10L;
	@Unique
	private static final Map<Villager, Long> TIMOTHATYS_TRINKETS$NEXT_NECROMANCY_SCAN = new WeakHashMap<>();

	@Inject(method = "hasHostile", at = @At("RETURN"), cancellable = true, require = 0)
	private static void timothatys_trinkets$panicNearNecromancy(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (Boolean.TRUE.equals(cir.getReturnValue())
				|| !(entity instanceof Villager villager)
				|| entity.level().isClientSide()) {
			return;
		}

		long gameTime = entity.level().getGameTime();
		long nextScan = TIMOTHATYS_TRINKETS$NEXT_NECROMANCY_SCAN.getOrDefault(villager, Long.MIN_VALUE);
		if (gameTime < nextScan)
			return;
		TIMOTHATYS_TRINKETS$NEXT_NECROMANCY_SCAN.put(
				villager,
				gameTime + TIMOTHATYS_TRINKETS$NEGATIVE_SCAN_INTERVAL_TICKS
		);

		LivingEntity panicSource = timothatys_trinkets$findNecromancyPanicSource(entity);
		if (panicSource != null) {
			TIMOTHATYS_TRINKETS$NEXT_NECROMANCY_SCAN.remove(villager);
			entity.getBrain().setMemoryWithExpiry(MemoryModuleType.NEAREST_HOSTILE, panicSource, TIMOTHATYS_TRINKETS$NECROMANCY_PANIC_MEMORY_TICKS);
			cir.setReturnValue(true);
		}
	}

	@Unique
	private static LivingEntity timothatys_trinkets$findNecromancyPanicSource(LivingEntity entity) {
		AABB panicBounds = entity.getBoundingBox().inflate(TIMOTHATYS_TRINKETS$NECROMANCY_PANIC_RADIUS);
		LivingEntity closestSource = null;
		double closestDistanceSqr = Double.MAX_VALUE;

		for (NecromancerEntity necromancer : entity.level().getEntitiesOfClass(
			NecromancerEntity.class,
			panicBounds,
			necromancer -> necromancer.isAlive() && entity.distanceToSqr(necromancer) <= TIMOTHATYS_TRINKETS$NECROMANCY_PANIC_RADIUS_SQR
		)) {
			double distanceSqr = entity.distanceToSqr(necromancer);
			if (distanceSqr < closestDistanceSqr) {
				closestSource = necromancer;
				closestDistanceSqr = distanceSqr;
			}
		}

		for (Villager villager : entity.level().getEntitiesOfClass(
			Villager.class,
			panicBounds,
			villager -> villager != entity
				&& villager.isAlive()
				&& entity.distanceToSqr(villager) <= TIMOTHATYS_TRINKETS$NECROMANCY_PANIC_RADIUS_SQR
				&& villager.hasEffect(TimothatysTrinketsModMobEffects.UNDEADIFICATION)
		)) {
			double distanceSqr = entity.distanceToSqr(villager);
			if (distanceSqr < closestDistanceSqr) {
				closestSource = villager;
				closestDistanceSqr = distanceSqr;
			}
		}

		return closestSource;
	}
}
