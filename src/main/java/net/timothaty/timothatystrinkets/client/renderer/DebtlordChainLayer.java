package net.timothaty.timothatystrinkets.client.renderer;

import net.timothaty.timothatystrinkets.client.model.ModelDebtlord;
import net.timothaty.timothatystrinkets.client.vfx.debtlord_chains.DebtlordChainVisualHandler;
import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordChainsGoal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class DebtlordChainLayer extends RenderLayer<DebtlordEntity, ModelDebtlord<DebtlordEntity>> {
	public DebtlordChainLayer(RenderLayerParent<DebtlordEntity, ModelDebtlord<DebtlordEntity>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, DebtlordEntity entity,
			float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		if (entity.getChainPhase() == DebtlordEntity.CHAIN_PHASE_FAILED) {
			DebtlordChainVisualHandler.discard(entity);
			return;
		}
		if (entity.getRenderAlpha(partialTick) <= 0.01F || !shouldRenderChain(entity))
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		Entity targetEntity = minecraft.level.getEntity(entity.getChainTargetId());
		if (!(targetEntity instanceof LivingEntity target))
			return;

		Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
		List<Vec3> sourcePositions = new ArrayList<>(2);
		for (Vector3f renderPosition : getParentModel().getChainSourceRenderPositions(poseStack)) {
			sourcePositions.add(new Vec3(
				cameraPosition.x + renderPosition.x,
				cameraPosition.y + renderPosition.y,
				cameraPosition.z + renderPosition.z));
		}
		DebtlordChainVisualHandler.record(entity, sourcePositions, target, partialTick);
	}

	private static boolean shouldRenderChain(DebtlordEntity entity) {
		if (!entity.isUsingChains())
			return false;
		if (entity.getChainPhase() == DebtlordEntity.CHAIN_PHASE_CAST) {
			int elapsedTicks = DebtlordChainsGoal.CAST_DURATION_TICKS - entity.getChainCastTicks() + 1;
			return elapsedTicks >= DebtlordChainsGoal.RELEASE_TICK;
		}
		return entity.getChainPhase() == DebtlordEntity.CHAIN_PHASE_SUCCESS;
	}
}
