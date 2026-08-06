package net.timothaty.timothatystrinkets.client.renderer;

import net.timothaty.timothatystrinkets.client.model.ModelDebtlord;
import net.timothaty.timothatystrinkets.client.vfx.debtlord_cast.DebtlordCastVfxHandler;
import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.entity.DebtlordPhase;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordChainsGoal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class DebtlordCastVfxLayer extends RenderLayer<DebtlordEntity, ModelDebtlord<DebtlordEntity>> {
	private static final DustParticleOptions DESOLATION_HAND_DUST = new DustParticleOptions(new Vector3f(0.08F, 0.01F, 0.11F), 1.05F);
	private static final DustParticleOptions PHASE_TWO_DUST = new DustParticleOptions(new Vector3f(0.025F, 0.006F, 0.035F), 0.85F);
	private static final DustParticleOptions PHASE_THREE_DUST = new DustParticleOptions(new Vector3f(0.045F, 0.004F, 0.055F), 1.12F);
	private static final DustParticleOptions LASER_WARNING_DUST = new DustParticleOptions(new Vector3f(0.78F, 0.0F, 0.055F), 0.86F);
	private static final Map<DebtlordEntity, Integer> LAST_DESOLATION_DUST_TICK = new WeakHashMap<>();
	private static final Map<DebtlordEntity, Integer> LAST_PHASE_DUST_TICK = new WeakHashMap<>();
	private static final Map<DebtlordEntity, Integer> LAST_LASER_WARNING_DUST_TICK = new WeakHashMap<>();

	public DebtlordCastVfxLayer(RenderLayerParent<DebtlordEntity, ModelDebtlord<DebtlordEntity>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, DebtlordEntity entity,
			float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		if (entity.getRenderAlpha(partialTick) <= 0.01F)
			return;
		boolean chainWarning = isChainWarningActive(entity);
		boolean desolation = entity.isDesolationAnimationActive();
		boolean phaseDust = shouldSpawnPhaseDust(entity);
		boolean laserWarning = entity.isFingerOfDeathChargeAnimationActive();
		if (!chainWarning && !desolation && !phaseDust && !laserWarning)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		if (phaseDust)
			spawnPhaseDust(entity);

		Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
		Vec3 leftArmPosition = Vec3.ZERO;
		if (chainWarning || desolation) {
			List<Vector3f> sourcePositions = getParentModel().getChainSourceRenderPositions(poseStack);
			if (!sourcePositions.isEmpty()) {
				Vector3f renderPosition = averagePosition(sourcePositions);
				leftArmPosition = new Vec3(
					cameraPosition.x + renderPosition.x,
					cameraPosition.y + renderPosition.y,
					cameraPosition.z + renderPosition.z);
				if (desolation)
					spawnDesolationHandDust(entity, leftArmPosition);
			}
		}

		Vec3 laserWarningPosition = Vec3.ZERO;
		if (laserWarning) {
			List<Vec3> fingerPositions = toWorldPositions(getParentModel().getFingerOfDeathLaserRenderPositions(poseStack), cameraPosition);
			if (!fingerPositions.isEmpty()) {
				laserWarningPosition = fingerPositions.get(fingerPositions.size() - 1);
				spawnLaserWarningDust(entity, laserWarningPosition);
			}
		}

		if (chainWarning || desolation || laserWarning)
			DebtlordCastVfxHandler.record(entity, leftArmPosition, chainWarning, desolation, laserWarningPosition, laserWarning, partialTick);
	}

	private static boolean isChainWarningActive(DebtlordEntity entity) {
		if (!entity.isChainCastAnimationActive())
			return false;

		int elapsedTicks = DebtlordChainsGoal.CAST_DURATION_TICKS - entity.getChainCastTicks() + 1;
		return elapsedTicks < DebtlordChainsGoal.RELEASE_TICK;
	}

	private static Vector3f averagePosition(List<Vector3f> positions) {
		Vector3f result = new Vector3f();
		for (Vector3f position : positions)
			result.add(position);
		return result.div(positions.size());
	}

	private static boolean shouldSpawnPhaseDust(DebtlordEntity entity) {
		return entity.isEnraged()
			&& entity.isAlive()
			&& !entity.isDeathAnimationActive();
	}

	private static List<Vec3> toWorldPositions(List<Vector3f> renderPositions, Vec3 cameraPosition) {
		List<Vec3> positions = new ArrayList<>(renderPositions.size());
		for (Vector3f renderPosition : renderPositions) {
			positions.add(new Vec3(
				cameraPosition.x + renderPosition.x,
				cameraPosition.y + renderPosition.y,
				cameraPosition.z + renderPosition.z));
		}
		return positions;
	}

	private static void spawnPhaseDust(DebtlordEntity entity) {
		Integer lastTick = LAST_PHASE_DUST_TICK.get(entity);
		if (lastTick != null && lastTick == entity.tickCount)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		LAST_PHASE_DUST_TICK.put(entity, entity.tickCount);
		RandomSource random = entity.getRandom();
		double halfWidth = entity.getBbWidth() * 0.5D;
		boolean phaseThree = entity.getPhase() == DebtlordPhase.PHASE_THREE;
		int count = phaseThree ? 5 : 2;
		DustParticleOptions dust = phaseThree ? PHASE_THREE_DUST : PHASE_TWO_DUST;
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double radius = halfWidth * (0.55D + random.nextDouble() * (phaseThree ? 0.95D : 0.62D));
			double y = entity.getY() + entity.getBbHeight() * (0.10D + random.nextDouble() * 0.82D);
			minecraft.level.addParticle(
				dust,
				entity.getX() + Math.cos(angle) * radius,
				y,
				entity.getZ() + Math.sin(angle) * radius,
				(random.nextDouble() - 0.5D) * (phaseThree ? 0.012D : 0.006D),
				0.003D + random.nextDouble() * (phaseThree ? 0.012D : 0.007D),
				(random.nextDouble() - 0.5D) * (phaseThree ? 0.012D : 0.006D));
		}
	}

	private static void spawnLaserWarningDust(DebtlordEntity entity, Vec3 position) {
		Integer lastTick = LAST_LASER_WARNING_DUST_TICK.get(entity);
		if (lastTick != null && lastTick == entity.tickCount)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		LAST_LASER_WARNING_DUST_TICK.put(entity, entity.tickCount);
		RandomSource random = entity.getRandom();
		for (int i = 0; i < 2; i++) {
			minecraft.level.addParticle(
				LASER_WARNING_DUST,
				position.x + (random.nextDouble() - 0.5D) * 0.10D,
				position.y + (random.nextDouble() - 0.5D) * 0.10D,
				position.z + (random.nextDouble() - 0.5D) * 0.10D,
				(random.nextDouble() - 0.5D) * 0.012D,
				(random.nextDouble() - 0.5D) * 0.012D,
				(random.nextDouble() - 0.5D) * 0.012D);
		}
	}

	private static void spawnDesolationHandDust(DebtlordEntity entity, Vec3 leftArmPosition) {
		Integer lastTick = LAST_DESOLATION_DUST_TICK.get(entity);
		if (lastTick != null && lastTick == entity.tickCount)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		LAST_DESOLATION_DUST_TICK.put(entity, entity.tickCount);
		RandomSource random = entity.getRandom();
		for (int i = 0; i < 2; i++) {
			minecraft.level.addParticle(
				DESOLATION_HAND_DUST,
				leftArmPosition.x + (random.nextDouble() - 0.5D) * 0.09D,
				leftArmPosition.y + (random.nextDouble() - 0.5D) * 0.09D,
				leftArmPosition.z + (random.nextDouble() - 0.5D) * 0.09D,
				(random.nextDouble() - 0.5D) * 0.008D,
				0.004D + random.nextDouble() * 0.006D,
				(random.nextDouble() - 0.5D) * 0.008D);
		}
	}
}
