package net.timothaty.timothatystrinkets.client.renderer.curio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class VanillaFirstPersonArmHelper {
	private static PlayerModel<AbstractClientPlayer> widePoseModel;
	private static PlayerModel<AbstractClientPlayer> slimPoseModel;

	private VanillaFirstPersonArmHelper() {
	}

	public static PlayerModel<AbstractClientPlayer> prepareVanillaModel(AbstractClientPlayer player) {
		PlayerModel<AbstractClientPlayer> model = getPoseModel(player);
		model.attackTime = 0.0F;
		model.crouching = false;
		model.swimAmount = 0.0F;
		model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
		model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
		model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
		model.rightArm.xRot = 0.0F;
		model.leftArm.xRot = 0.0F;
		return model;
	}

	private static PlayerModel<AbstractClientPlayer> getPoseModel(AbstractClientPlayer player) {
		if (player.getSkin().model() == PlayerSkin.Model.SLIM) {
			if (slimPoseModel == null) {
				slimPoseModel = new PlayerModel<>(
						Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
			}
			return slimPoseModel;
		}

		if (widePoseModel == null) {
			widePoseModel = new PlayerModel<>(
					Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
		}
		return widePoseModel;
	}
}
