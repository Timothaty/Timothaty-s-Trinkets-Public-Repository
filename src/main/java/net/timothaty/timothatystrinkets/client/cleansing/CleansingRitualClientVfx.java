package net.timothaty.timothatystrinkets.client.cleansing;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.CleansingRitualControllerEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.mechanics.cleansing.ritual.CleansingRitualPattern;
import net.timothaty.timothatystrinkets.mechanics.cleansing.ritual.CleansingRitualVisualBridge;
import net.timothaty.timothatystrinkets.particle.RitualSmokeParticleOptions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import org.joml.Vector3f;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class CleansingRitualClientVfx {
	private static final double MAX_DISTANCE_SQUARED = 64.0D * 64.0D;
	private static final float GOLD_RED = 1.0F;
	private static final float GOLD_GREEN = 0.74F;
	private static final float GOLD_BLUE = 0.20F;

	private CleansingRitualClientVfx() {
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> CleansingRitualVisualBridge.install(CleansingRitualClientVfx::tick));
	}

	public static void tick(CleansingRitualControllerEntity controller) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (player == null || !(controller.level() instanceof ClientLevel level)
				|| player.distanceToSqr(controller) > MAX_DISTANCE_SQUARED) return;

		ParticleStatus status = minecraft.options.particles().get();
		switch (controller.getPhase()) {
			case BURNING -> tickBurning(controller, level, status);
			case CONSECRATION -> tickConsecration(controller, level, status);
			case COMPLETE -> showFinalBurst(controller, level, status);
		}
	}

	private static void tickBurning(CleansingRitualControllerEntity controller, ClientLevel level, ParticleStatus status) {
		if (status != ParticleStatus.MINIMAL || (controller.tickCount & 1) == 0) {
			spawnMovingFlame(controller, level, status == ParticleStatus.ALL ? 2 : 1);
		}
		if (controller.consumeClientStepChange()) spawnStepSmoke(controller, level, status);

		int smokeInterval = status == ParticleStatus.ALL ? 2 : status == ParticleStatus.DECREASED ? 4 : 8;
		if (controller.getCompletedSteps() > 0 && controller.tickCount % smokeInterval == 0) {
			spawnAttractingSmoke(controller, level, controller.getCompletedSteps());
		}
	}

	private static void tickConsecration(CleansingRitualControllerEntity controller, ClientLevel level, ParticleStatus status) {
		int smokeInterval = status == ParticleStatus.ALL ? 1 : status == ParticleStatus.DECREASED ? 2 : 5;
		if (controller.tickCount % smokeInterval == 0) {
			spawnAttractingSmoke(controller, level, CleansingRitualPattern.INCENSE_COUNT);
			if (status != ParticleStatus.MINIMAL || (controller.tickCount & 1) == 0) spawnPotSmoke(controller, level);
		}

		float progress = Mth.clamp(controller.getClientPhaseAge(0.0F) / CleansingRitualControllerEntity.CONSECRATION_TICKS, 0.0F, 1.0F);
		int interval = Math.max(1, 6 - Mth.floor(progress * 5.0F));
		if (status == ParticleStatus.DECREASED) interval *= 2;
		if (status == ParticleStatus.MINIMAL) interval *= 4;
		if (controller.tickCount % interval == 0) spawnGoldenDot(controller, level, progress);
	}

	private static void spawnMovingFlame(CleansingRitualControllerEntity controller, ClientLevel level, int count) {
		int currentIndex = CleansingRitualPattern.wrappedRouteIndex(controller.getStartRouteIndex(), controller.getCompletedSteps());
		int nextIndex = (currentIndex + 1) % CleansingRitualPattern.INCENSE_COUNT;
		BlockPos from = CleansingRitualPattern.CLOCKWISE_ROUTE.get(currentIndex);
		BlockPos to = CleansingRitualPattern.CLOCKWISE_ROUTE.get(nextIndex);
		BlockPos center = controller.getCenter();
		double progress = controller.getClientStepProgress(0.0F);
		double x = center.getX() + 0.5D + Mth.lerp(progress, from.getX(), to.getX());
		double z = center.getZ() + 0.5D + Mth.lerp(progress, from.getZ(), to.getZ());
		for (int i = 0; i < count; i++) {
			level.addParticle(ParticleTypes.FLAME,
					x + (level.random.nextDouble() - 0.5D) * 0.08D,
					center.getY() + 0.10D + level.random.nextDouble() * 0.07D,
					z + (level.random.nextDouble() - 0.5D) * 0.08D,
					0.0D, 0.004D, 0.0D);
		}
	}

	private static void spawnStepSmoke(CleansingRitualControllerEntity controller, ClientLevel level, ParticleStatus status) {
		int burnedIndex = CleansingRitualPattern.wrappedRouteIndex(controller.getStartRouteIndex(), controller.getCompletedSteps() - 1);
		BlockPos offset = CleansingRitualPattern.CLOCKWISE_ROUTE.get(burnedIndex);
		BlockPos center = controller.getCenter();
		int count = status == ParticleStatus.ALL ? 3 : 1;
		for (int i = 0; i < count; i++) {
			level.addParticle(ParticleTypes.SMOKE,
					center.getX() + offset.getX() + 0.5D,
					center.getY() + 0.10D,
					center.getZ() + offset.getZ() + 0.5D,
					0.0D, 0.012D, 0.0D);
		}
	}

	private static void spawnAttractingSmoke(CleansingRitualControllerEntity controller, ClientLevel level, int availableSteps) {
		int relativeStep = level.random.nextInt(Math.max(1, availableSteps));
		int routeIndex = CleansingRitualPattern.wrappedRouteIndex(controller.getStartRouteIndex(), relativeStep);
		BlockPos offset = CleansingRitualPattern.CLOCKWISE_ROUTE.get(routeIndex);
		BlockPos center = controller.getCenter();
		double sourceX = center.getX() + offset.getX() + 0.5D;
		double sourceY = center.getY() + 0.10D;
		double sourceZ = center.getZ() + offset.getZ() + 0.5D;
		level.addParticle(new RitualSmokeParticleOptions(new Vector3f(
				(float)(center.getX() + 0.5D - sourceX), 0.75F,
				(float)(center.getZ() + 0.5D - sourceZ))),
				sourceX, sourceY, sourceZ, 0.0D, 0.0D, 0.0D);
	}

	private static void spawnPotSmoke(CleansingRitualControllerEntity controller, ClientLevel level) {
		BlockPos center = controller.getCenter();
		double x = center.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.18D;
		double y = center.getY() + 1.05D;
		double z = center.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.18D;
		level.addParticle(new RitualSmokeParticleOptions(new Vector3f(
				(float)(center.getX() + 0.5D - x), 0.55F, (float)(center.getZ() + 0.5D - z))),
				x, y, z, 0.0D, 0.0D, 0.0D);
	}

	private static void spawnGoldenDot(CleansingRitualControllerEntity controller, ClientLevel level, float progress) {
		BlockPos center = controller.getCenter();
		double angle = level.random.nextDouble() * Math.PI * 2.0D;
		double radius = 0.45D + level.random.nextDouble() * (0.55D - progress * 0.15D);
		level.addParticle(TimothatysTrinketsModParticleTypes.DOT.get(),
				center.getX() + 0.5D + Math.cos(angle) * radius,
				center.getY() + 0.25D + level.random.nextDouble() * 1.15D,
				center.getZ() + 0.5D + Math.sin(angle) * radius,
				GOLD_RED, GOLD_GREEN, GOLD_BLUE);
	}

	private static void showFinalBurst(CleansingRitualControllerEntity controller, ClientLevel level, ParticleStatus status) {
		if (!controller.markClientFinalBurstShown()) return;
		BlockPos center = controller.getCenter();
		int count = status == ParticleStatus.ALL ? 18 : status == ParticleStatus.DECREASED ? 10 : 5;
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2.0D * i / count + level.random.nextDouble() * 0.18D;
			double speed = 0.035D + level.random.nextDouble() * 0.035D;
			level.addParticle(ParticleTypes.SMOKE,
					center.getX() + 0.5D, center.getY() + 0.72D, center.getZ() + 0.5D,
					Math.cos(angle) * speed, 0.018D + level.random.nextDouble() * 0.025D, Math.sin(angle) * speed);
		}
	}
}
