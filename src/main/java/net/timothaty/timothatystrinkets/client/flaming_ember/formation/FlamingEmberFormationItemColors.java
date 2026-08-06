package net.timothaty.timothatystrinkets.client.flaming_ember.formation;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.flaming_ember.formation.FlamingEmberFormationData;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class FlamingEmberFormationItemColors {
	private static final int WHITE = 0xFFFFFF;
	private static final int LIGHT_RED = 0xFFD5CF;
	private static final int RED = 0xFF7462;
	private static final int ORANGE = 0xFF8A28;

	private FlamingEmberFormationItemColors() {
	}

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register(FlamingEmberFormationItemColors::getColor, Items.CHARCOAL);
	}

	private static int getColor(ItemStack stack, int tintIndex) {
		if (tintIndex != 0 || Minecraft.getInstance().level == null || Minecraft.getInstance().player == null)
			return -1;

		long currentGameTime = Minecraft.getInstance().level.getGameTime();
		int visualProgress = FlamingEmberFormationData.getFreshVisualProgress(
				Minecraft.getInstance().player, stack, currentGameTime);
		if (visualProgress < 0)
			return -1;

		float progress = visualProgress / (float) FlamingEmberFormationData.DURATION_TICKS;
		if (progress <= 0.5F)
			return interpolate(WHITE, LIGHT_RED, smoothstep(progress / 0.5F));
		if (progress <= 0.8F)
			return interpolate(LIGHT_RED, RED, smoothstep((progress - 0.5F) / 0.3F));
		return interpolate(RED, ORANGE, smoothstep((progress - 0.8F) / 0.2F));
	}

	private static int interpolate(int from, int to, float amount) {
		int red = Math.round(channel(from, 16) + (channel(to, 16) - channel(from, 16)) * amount);
		int green = Math.round(channel(from, 8) + (channel(to, 8) - channel(from, 8)) * amount);
		int blue = Math.round(channel(from, 0) + (channel(to, 0) - channel(from, 0)) * amount);
		return 0xFF000000 | red << 16 | green << 8 | blue;
	}

	private static int channel(int color, int shift) {
		return color >> shift & 0xFF;
	}

	private static float smoothstep(float value) {
		return value * value * (3.0F - 2.0F * value);
	}
}
