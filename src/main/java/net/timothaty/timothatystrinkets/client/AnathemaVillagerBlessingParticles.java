package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.mechanics.anathema.AnathemaVillagerBlessingState;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.model.geom.ModelPart;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public final class AnathemaVillagerBlessingParticles {
	private static final DustParticleOptions BLESSING_DUST = new DustParticleOptions(
		new Vector3f(1.0F, 223.0F / 255.0F, 132.0F / 255.0F),
		0.75F
	);

	private static final float HAND_JOIN_X = -1.0F;
	private static final float HAND_JOIN_Y = 4.0F;
	private static final float HAND_JOIN_Z = -2.25F;
	private static final double MODEL_GROUND_OFFSET = 1.501D;

	private AnathemaVillagerBlessingParticles() {
	}

	public static void emit(Entity entity, AnathemaVillagerBlessingState state, float ageInTicks, ModelPart root) {
		if (!(entity instanceof LivingEntity living)
				|| !entity.level().isClientSide()
				|| !state.timothatys_trinkets$claimBlessingsParticleTick(entity.tickCount))
			return;

		ModelPart arms = root.getChild("arms");
		PoseStack modelPose = new PoseStack();
		root.translateAndRotate(modelPose);
		arms.translateAndRotate(modelPose);

		Vector4f locator = new Vector4f(
			HAND_JOIN_X / 16.0F,
			HAND_JOIN_Y / 16.0F,
			HAND_JOIN_Z / 16.0F,
			1.0F
		);
		modelPose.last().pose().transform(locator);

		float partialTick = Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F);
		float bodyYaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
		double yawRadians = Math.toRadians(180.0D - bodyYaw);
		double cosYaw = Math.cos(yawRadians);
		double sinYaw = Math.sin(yawRadians);
		double modelX = -locator.x();
		double modelY = MODEL_GROUND_OFFSET - locator.y();
		double modelZ = locator.z();
		double rotatedX = modelX * cosYaw + modelZ * sinYaw;
		double rotatedZ = -modelX * sinYaw + modelZ * cosYaw;

		double x = entity.getX() + rotatedX;
		double y = entity.getY() + modelY;
		double z = entity.getZ() + rotatedZ;
		double spreadX = (entity.getRandom().nextDouble() - 0.5D) * 0.025D;
		double spreadY = (entity.getRandom().nextDouble() - 0.5D) * 0.02D;
		double spreadZ = (entity.getRandom().nextDouble() - 0.5D) * 0.025D;
		entity.level().addParticle(BLESSING_DUST, x + spreadX, y + spreadY, z + spreadZ, 0.0D, 0.0D, 0.0D);
	}
}
