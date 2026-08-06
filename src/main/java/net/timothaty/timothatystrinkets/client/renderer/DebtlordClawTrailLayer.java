package net.timothaty.timothatystrinkets.client.renderer;

import net.timothaty.timothatystrinkets.client.model.ModelDebtlord;
import net.timothaty.timothatystrinkets.client.vfx.debtlord_claws.DebtlordClawTrailHandler;
import net.timothaty.timothatystrinkets.entity.DebtlordEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class DebtlordClawTrailLayer extends RenderLayer<DebtlordEntity, ModelDebtlord<DebtlordEntity>> {
	public DebtlordClawTrailLayer(RenderLayerParent<DebtlordEntity, ModelDebtlord<DebtlordEntity>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, DebtlordEntity entity,
			float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		if (entity.getRenderAlpha(partialTick) <= 0.01F || !entity.isClawAnimationActive())
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
		List<Vec3> fingerPositions = new ArrayList<>(8);
		for (Vector3f renderPosition : getParentModel().getClawFingerRenderPositions(poseStack)) {
			fingerPositions.add(new Vec3(
				cameraPosition.x + renderPosition.x,
				cameraPosition.y + renderPosition.y,
				cameraPosition.z + renderPosition.z));
		}
		DebtlordClawTrailHandler.record(entity, fingerPositions);
	}
}
