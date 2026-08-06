package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class CooldownColourMixin {
	@Unique
	private static final int timothatys_trinkets$PURPLE_COOLDOWN_COLOR = 0xAA8B3CFF;
	@Unique
	private static final int timothatys_trinkets$CYAN_COOLDOWN_COLOR = 0xAA18D8FF;
	@Unique
	private static final int timothatys_trinkets$RED_COOLDOWN_COLOR = 0xAAC40018;
	@Unique
	private static final int timothatys_trinkets$YELLOW_COOLDOWN_COLOR = 0xAAFFD84A;
	@Unique
	private static final int timothatys_trinkets$ORANGE_COOLDOWN_COLOR = 0xAAFF6A00;
	@Unique
	private static final int timothatys_trinkets$SOUL_CYAN_COOLDOWN_COLOR = 0xAA08E8DE;
	@Unique
	private static final int timothatys_trinkets$GREEN_COOLDOWN_COLOR = 0xAA00A848;
	@Unique
	private static final int timothatys_trinkets$CHAMPION_PURPLE_COOLDOWN_COLOR = 0xAA4A12B8;
	@Unique
	private static final int timothatys_trinkets$REFRESHING_CHALICE_COOLDOWN_COLOR = 0xAACC0605;
	@Unique
	private static final int timothatys_trinkets$STRIKER_GOLD_COOLDOWN_COLOR = 0xAAFFE19A;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	public abstract void fill(RenderType renderType, int minX, int minY, int maxX, int maxY, int color);

	@Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"), require = 0)
	private void timothatys_trinkets$renderCustomCooldownColor(Font font, ItemStack stack, int x, int y, @Nullable String text, CallbackInfo ci) {
		if (stack.isEmpty()) {
			return;
		}

		int cooldownColor = timothatys_trinkets$getCooldownColor(stack);
		if (cooldownColor == 0) {
			return;
		}

		LocalPlayer localPlayer = this.minecraft.player;
		float cooldown = localPlayer == null ? 0.0F : localPlayer.getCooldowns().getCooldownPercent(stack.getItem(), this.minecraft.getTimer().getGameTimeDeltaPartialTick(true));
		if (cooldown <= 0.0F) {
			return;
		}

		int cooldownTop = y + Mth.floor(16.0F * (1.0F - cooldown));
		int cooldownBottom = cooldownTop + Mth.ceil(16.0F * cooldown);
		this.fill(RenderType.guiOverlay(), x, cooldownTop, x + 16, cooldownBottom, cooldownColor);
	}

	@Unique
	private static int timothatys_trinkets$getCooldownColor(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		if (stack.getItem() == TimothatysTrinketsModItems.VOID_SPHERE.get()) {
			return timothatys_trinkets$PURPLE_COOLDOWN_COLOR;
		}
		if (stack.getItem() == TimothatysTrinketsModItems.ECHO_SPHERE.get()) {
			return timothatys_trinkets$CYAN_COOLDOWN_COLOR;
		}
		if (stack.getItem() == TimothatysTrinketsModItems.FANGS.get()) {
			return timothatys_trinkets$RED_COOLDOWN_COLOR;
		}
		if (stack.getItem() == TimothatysTrinketsModItems.GOLDEN_HONEY_COMB.get()) {
			return timothatys_trinkets$YELLOW_COOLDOWN_COLOR;
		}
		if (stack.getItem() == TimothatysTrinketsModItems.FIRE_SPHERE.get()) {
			return timothatys_trinkets$ORANGE_COOLDOWN_COLOR;
		}
		if (stack.getItem() == TimothatysTrinketsModItems.UNDEAD_KNIGHTS_ARMLET.get()) {
			return timothatys_trinkets$SOUL_CYAN_COOLDOWN_COLOR;
		}
		if (stack.getItem() == TimothatysTrinketsModItems.CHAMPIONS_GAUNTLET.get()) {
			return timothatys_trinkets$CHAMPION_PURPLE_COOLDOWN_COLOR;
		}
		if (stack.getItem() == TimothatysTrinketsModItems.PAGANS_CHARM.get()) {
			return timothatys_trinkets$GREEN_COOLDOWN_COLOR;
		}
		if (stack.getItem() == TimothatysTrinketsModItems.REFRESHING_CHALICE.get()) {
			return timothatys_trinkets$REFRESHING_CHALICE_COOLDOWN_COLOR;
		}
		if (stack.getItem() == TimothatysTrinketsModItems.STRIKER_OF_THE_MORNING_STAR.get()) {
			return timothatys_trinkets$STRIKER_GOLD_COOLDOWN_COLOR;
		}
		return 0;
	}
}
