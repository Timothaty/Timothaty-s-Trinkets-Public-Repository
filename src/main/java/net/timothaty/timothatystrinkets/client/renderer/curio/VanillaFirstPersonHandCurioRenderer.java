package net.timothaty.timothatystrinkets.client.renderer.curio;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class VanillaFirstPersonHandCurioRenderer {
	private VanillaFirstPersonHandCurioRenderer() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public static void onRenderArm(RenderArmEvent event) {
		if (event.isCanceled())
			return;

		AbstractClientPlayer player = event.getPlayer();
		if (player == null || player.isSpectator())
			return;

		ResolvedHandCurioVisuals visuals = HandCurioVisualResolver.resolve(
				player,
				event.getArm(),
				HandCurioRenderContext.NORMAL_FIRST_PERSON
		);
		if (visuals.isEmpty())
			return;

		HumanoidModel<?> vanillaModel = VanillaFirstPersonArmHelper.prepareVanillaModel(player);
		HandCurioModelRenderer.INSTANCE.render(
				player,
				event.getArm(),
				vanillaModel,
				visuals,
				event.getPoseStack(),
				event.getMultiBufferSource(),
				event.getPackedLight()
		);
	}
}
