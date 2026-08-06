package net.timothaty.timothatystrinkets.client.renderer;

import net.timothaty.timothatystrinkets.client.model.ModelDebtlord;
import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.Map;
import java.util.WeakHashMap;

public final class DebtlordDeathBloodLayer extends RenderLayer<DebtlordEntity, ModelDebtlord<DebtlordEntity>> {
	private static final int PARTICLE_END_TICK = 156;
	private final Map<DebtlordEntity, Integer> lastEmissionTicks = new WeakHashMap<>();

	public DebtlordDeathBloodLayer(RenderLayerParent<DebtlordEntity, ModelDebtlord<DebtlordEntity>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, DebtlordEntity entity,
			float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		int deathTicks = entity.getDeathAnimationTicks();
		if (deathTicks <= 0 || deathTicks > PARTICLE_END_TICK || lastEmissionTicks.getOrDefault(entity, Integer.MIN_VALUE) == entity.tickCount)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;
		lastEmissionTicks.put(entity, entity.tickCount);

		Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
		RandomSource random = entity.getRandom();
		for (org.joml.Vector3f renderPosition : getParentModel().getDeathBloodRenderPositions(poseStack)) {
			double x = cameraPosition.x + renderPosition.x + (random.nextDouble() - 0.5D) * 0.18D;
			double y = cameraPosition.y + renderPosition.y + (random.nextDouble() - 0.5D) * 0.18D;
			double z = cameraPosition.z + renderPosition.z + (random.nextDouble() - 0.5D) * 0.18D;
			minecraft.level.addParticle(
				TimothatysTrinketsModParticleTypes.BLOOD_BIT.get(),
				x, y, z,
				(random.nextDouble() - 0.5D) * 0.22D,
				0.04D + random.nextDouble() * 0.18D,
				(random.nextDouble() - 0.5D) * 0.22D
			);
		}
	}
}
