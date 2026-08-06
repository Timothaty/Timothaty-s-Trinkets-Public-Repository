package net.timothaty.timothatystrinkets.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.timothaty.timothatystrinkets.client.hubris.PlayerHandAnchorTracker;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class HubrisFirstPersonHandAnchorMixin {
	@Inject(method = "renderItem", at = @At("HEAD"))
	private void timothatys_trinkets$captureHubrisHandAnchor(
			LivingEntity entity,
			ItemStack stack,
			ItemDisplayContext context,
			boolean leftHand,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			int packedLight,
			CallbackInfo ci
	) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (entity != player || player == null || !player.hasEffect(TimothatysTrinketsModMobEffects.HUBRIS))
			return;
		if (context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
				&& context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
			return;
		HumanoidArm arm = leftHand ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
		if (arm != player.getMainArm() || (!stack.is(ItemTags.SWORDS) && !stack.is(HubrisData.HEAVY_ARMS)))
			return;
		PlayerHandAnchorTracker.captureFirstPerson(player, poseStack, arm);
	}
}
