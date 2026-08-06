package net.timothaty.timothatystrinkets.client.renderer;

import net.timothaty.timothatystrinkets.client.model.ModelDebtlord;
import net.timothaty.timothatystrinkets.client.vfx.debtlord_finger.FingerOfDeathLaserHandler;
import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordFingerOfDeathGoal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;

import org.joml.Vector3f;

import java.util.List;

public final class DebtlordFingerOfDeathLayer extends RenderLayer<DebtlordEntity, ModelDebtlord<DebtlordEntity>> {
	public DebtlordFingerOfDeathLayer(RenderLayerParent<DebtlordEntity, ModelDebtlord<DebtlordEntity>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, DebtlordEntity entity,
			float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		if (entity.getRenderAlpha(partialTick) <= 0.01F || !entity.isFingerOfDeathIdleAnimationActive())
			return;

		Vec3 source = getFingerTipSource(poseStack, entity);
		Vec3 end = entity.getFingerOfDeathLaserTarget(partialTick);
		if (end.distanceToSqr(source) < 0.0025D)
			end = source.add(entity.getFingerOfDeathLaserDirection().scale(DebtlordFingerOfDeathGoal.LASER_RANGE));
		FingerOfDeathLaserHandler.record(entity, source, end, partialTick);
	}

	private Vec3 getFingerTipSource(PoseStack poseStack, DebtlordEntity entity) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return entity.getFingerOfDeathServerLaserSource();

		List<Vector3f> positions = getParentModel().getFingerOfDeathLaserRenderPositions(poseStack);
		if (positions.isEmpty())
			return entity.getFingerOfDeathServerLaserSource();

		Vector3f renderPosition = positions.get(positions.size() - 1);
		Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
		return new Vec3(
			cameraPosition.x + renderPosition.x,
			cameraPosition.y + renderPosition.y,
			cameraPosition.z + renderPosition.z
		);
	}
}
