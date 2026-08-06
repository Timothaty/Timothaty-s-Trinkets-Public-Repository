package net.timothaty.timothatystrinkets.client.animation;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class HumanoidAnimationRenderPassHelper {
	private HumanoidAnimationRenderPassHelper() {
	}

	public static boolean isVanillaFirstPersonArmPass(LivingEntity entity, float limbSwing, float limbSwingAmount,
			float ageInTicks, float netHeadYaw, float headPitch) {
		Minecraft minecraft = Minecraft.getInstance();
		return entity == minecraft.player
				&& minecraft.options.getCameraType() == CameraType.FIRST_PERSON
				&& limbSwing == 0.0F
				&& limbSwingAmount == 0.0F
				&& ageInTicks == 0.0F
				&& netHeadYaw == 0.0F
				&& headPitch == 0.0F;
	}
}
