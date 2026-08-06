package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.entity.layers.CustomHeadLayer")
public abstract class DebtlordHeadLayerMixin {
	@Unique
	private static final float timothatys_trinkets$DEBTLORD_HEAD_SCALE = 1.390620625F / 1.46289F;
	@Unique
	private static final float timothatys_trinkets$DEBTLORD_HEAD_Y_OFFSET = -0.5F / 16.0F;

	@Inject(
			method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;renderSkull(Lnet/minecraft/core/Direction;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/SkullModelBase;Lnet/minecraft/client/renderer/RenderType;)V"
			),
			require = 0
	)
	private void timothatys_trinkets$scaleWornDebtlordHead(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
			float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (!(entity instanceof Player) || !timothatys_trinkets$isWearingDebtlordHead(entity))
			return;

		poseStack.translate(0.0F, timothatys_trinkets$DEBTLORD_HEAD_Y_OFFSET, 0.0F);
		poseStack.scale(timothatys_trinkets$DEBTLORD_HEAD_SCALE, timothatys_trinkets$DEBTLORD_HEAD_SCALE, timothatys_trinkets$DEBTLORD_HEAD_SCALE);
	}

	@Unique
	private static boolean timothatys_trinkets$isWearingDebtlordHead(LivingEntity entity) {
		ItemStack headStack = entity.getItemBySlot(EquipmentSlot.HEAD);
		return headStack.is(TimothatysTrinketsModItems.DEBTLORDS_HEAD.get());
	}
}
