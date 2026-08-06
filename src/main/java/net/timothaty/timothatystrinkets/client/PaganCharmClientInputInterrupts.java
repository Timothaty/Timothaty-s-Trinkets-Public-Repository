package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModKeyMappings;
import net.timothaty.timothatystrinkets.mechanics.pagans_charm.PaganCharmMeditationPlayerState;
import net.timothaty.timothatystrinkets.network.PaganCharmMeditationInterruptMessage;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class PaganCharmClientInputInterrupts {
	private PaganCharmClientInputInterrupts() {
	}

	@SubscribeEvent
	public static void onKey(InputEvent.Key event) {
		if (event.getAction() != GLFW.GLFW_PRESS)
			return;
		if (!isActiveAbilityKey(event.getKey(), event.getScanCode()))
			return;

		Minecraft minecraft = Minecraft.getInstance();
		interruptMeditation(minecraft);
	}

	@SubscribeEvent
	public static void onMouseButton(InputEvent.MouseButton.Post event) {
		if (event.getAction() != GLFW.GLFW_PRESS)
			return;
		if (!isActiveAbilityMouseButton(event.getButton()))
			return;

		Minecraft minecraft = Minecraft.getInstance();
		interruptMeditation(minecraft);
	}

	private static void interruptMeditation(Minecraft minecraft) {
		if (minecraft.screen != null)
			return;
		LocalPlayer player = minecraft.player;
		if (!(player instanceof PaganCharmMeditationPlayerState state) || !state.timothatys_trinkets$isPaganCharmMeditationPrimed())
			return;

		PacketDistributor.sendToServer(PaganCharmMeditationInterruptMessage.INSTANCE);
		state.timothatys_trinkets$interruptPaganCharmMeditation();
	}

	private static boolean isActiveAbilityKey(int key, int scanCode) {
		return TimothatysTrinketsModKeyMappings.ACTIVE_ABILITY.matches(key, scanCode);
	}

	private static boolean isActiveAbilityMouseButton(int button) {
		return TimothatysTrinketsModKeyMappings.ACTIVE_ABILITY.matchesMouse(button);
	}
}
