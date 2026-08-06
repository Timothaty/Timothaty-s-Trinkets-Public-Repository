package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.NecromancerEntity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

public final class NecromancerSpellParticles {
	private static final DustParticleOptions UNDEADIFICATION_HAND_DUST = new DustParticleOptions(new Vector3f(0.16F, 0.0F, 0.28F), 1.15F);
	private static final float UNDEADIFICATION_ANIMATION_SECONDS = 1.75F;
	private static final float MODEL_ROOT_Y = 12.0F;
	private static final float MODEL_FEET_Y = 24.0F;
	private static final float LEFT_ARM_PIVOT_X = 6.0F;
	private static final float LEFT_ARM_PIVOT_Y = -9.6741F;
	private static final float LEFT_ARM_PIVOT_Z = 0.0995F;
	private static final float LEFT_ARM_TIP_LOCAL_X = 0.0F;
	private static final float LEFT_ARM_TIP_LOCAL_Y = 10.1595F;
	private static final float LEFT_ARM_TIP_LOCAL_Z = -0.0689F;
	private static final double PIXELS_TO_BLOCKS = 1.0D / 16.0D;
	private static final float[] LEFT_ARM_KEY_TIMES = {0.0F, 0.2917F, 0.75F, 1.0F, 1.75F};
	private static final float[] LEFT_ARM_X_ROT_DEGREES = {0.0F, -10.0F, -110.0F, -90.0F, 0.0F};
	private static final float[] LEFT_ARM_Y_OFFSETS = {0.0F, 0.0F, 0.0F, -1.0F, 0.0F};
	private static final float[] LEFT_ARM_Z_OFFSETS = {0.0F, -1.0F, -2.25F, -2.25F, 0.0F};

	private NecromancerSpellParticles() {
	}

	public static void spawnUndeadificationHandParticles(NecromancerEntity necromancer) {
		if (!necromancer.level().isClientSide()
				|| necromancer.isInvisible()
				|| !necromancer.isUndeadificationAnimationActive()) {
			return;
		}

		Vec3 tip = getLeftArmTipPosition(necromancer);
		for (int i = 0; i < 3; i++) {
			necromancer.level().addParticle(
					UNDEADIFICATION_HAND_DUST,
					tip.x + necromancer.getRandom().nextGaussian() * 0.035D,
					tip.y + necromancer.getRandom().nextGaussian() * 0.035D,
					tip.z + necromancer.getRandom().nextGaussian() * 0.035D,
					0.0D,
					0.0D,
					0.0D
			);
		}
	}

	private static Vec3 getLeftArmTipPosition(NecromancerEntity necromancer) {
		float animationTime = necromancer.getUndeadificationAnimationProgress() * UNDEADIFICATION_ANIMATION_SECONDS;
		float armXRot = interpolateCatmullRom(animationTime, LEFT_ARM_KEY_TIMES, LEFT_ARM_X_ROT_DEGREES) * Mth.DEG_TO_RAD;
		float armYOffset = interpolateCatmullRom(animationTime, LEFT_ARM_KEY_TIMES, LEFT_ARM_Y_OFFSETS);
		float armZOffset = interpolateCatmullRom(animationTime, LEFT_ARM_KEY_TIMES, LEFT_ARM_Z_OFFSETS);
		double rotatedTipY = LEFT_ARM_TIP_LOCAL_Y * Mth.cos(armXRot) - LEFT_ARM_TIP_LOCAL_Z * Mth.sin(armXRot);
		double rotatedTipZ = LEFT_ARM_TIP_LOCAL_Y * Mth.sin(armXRot) + LEFT_ARM_TIP_LOCAL_Z * Mth.cos(armXRot);
		double modelX = LEFT_ARM_PIVOT_X + LEFT_ARM_TIP_LOCAL_X;
		double modelY = MODEL_ROOT_Y + LEFT_ARM_PIVOT_Y + armYOffset + rotatedTipY;
		double modelZ = LEFT_ARM_PIVOT_Z + armZOffset + rotatedTipZ;
		double localX = modelX * PIXELS_TO_BLOCKS;
		double localY = (MODEL_FEET_Y - modelY) * PIXELS_TO_BLOCKS;
		double localZ = -modelZ * PIXELS_TO_BLOCKS;
		float yaw = necromancer.yBodyRot * Mth.DEG_TO_RAD;
		double sin = Mth.sin(yaw);
		double cos = Mth.cos(yaw);
		double worldX = necromancer.getX() + localX * cos - localZ * sin;
		double worldZ = necromancer.getZ() + localX * sin + localZ * cos;
		return new Vec3(worldX, necromancer.getY() + localY, worldZ);
	}

	private static float interpolateCatmullRom(float time, float[] times, float[] values) {
		if (time <= times[0]) {
			return values[0];
		}

		int lastIndex = times.length - 1;
		if (time >= times[lastIndex]) {
			return values[lastIndex];
		}

		int nextIndex = 1;
		while (nextIndex < times.length && time >= times[nextIndex]) {
			nextIndex++;
		}

		int currentIndex = nextIndex - 1;
		float progress = (time - times[currentIndex]) / (times[nextIndex] - times[currentIndex]);
		float previous = values[Math.max(0, currentIndex - 1)];
		float current = values[currentIndex];
		float next = values[nextIndex];
		float nextAfter = values[Math.min(lastIndex, nextIndex + 1)];
		return catmullRom(progress, previous, current, next, nextAfter);
	}

	private static float catmullRom(float progress, float previous, float current, float next, float nextAfter) {
		float progressSqr = progress * progress;
		float progressCube = progressSqr * progress;
		return 0.5F * (
			2.0F * current
				+ (next - previous) * progress
				+ (2.0F * previous - 5.0F * current + 4.0F * next - nextAfter) * progressSqr
				+ (3.0F * current - previous - 3.0F * next + nextAfter) * progressCube
		);
	}
}
