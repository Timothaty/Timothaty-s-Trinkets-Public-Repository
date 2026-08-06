package net.timothaty.timothatystrinkets.init;

import org.lwjgl.glfw.GLFW;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.timothaty.timothatystrinkets.network.ActiveAbilityMessage;
import net.timothaty.timothatystrinkets.client.hubris.HubrisActivationClientState;
import net.timothaty.timothatystrinkets.client.wrath_of_the_wicked.WrathOfTheWickedClientState;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

@EventBusSubscriber(Dist.CLIENT)
public class TimothatysTrinketsModKeyMappings {
	public static final KeyMapping ACTIVE_ABILITY = new KeyMapping("key.timothatys_trinkets.active_ability", GLFW.GLFW_KEY_R, "key.categories.misc") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				Minecraft minecraft = Minecraft.getInstance();
				var player = minecraft.player;
				if (player != null
						&& !TimothatysTrinketsStunHelper.isStunned(player)
						&& !HubrisActivationClientState.isCasting(player)
						&& !WrathOfTheWickedClientState.isActive(player)) {
					PacketDistributor.sendToServer(new ActiveAbilityMessage(0, 0));
					ActiveAbilityMessage.pressAction(player, 0, 0);
				}
			}
			isDownOld = isDown;
		}
	};

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(ACTIVE_ABILITY);
	}

	@EventBusSubscriber(Dist.CLIENT)
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			if (Minecraft.getInstance().screen == null) {
				ACTIVE_ABILITY.consumeClick();
			}
		}
	}
}
