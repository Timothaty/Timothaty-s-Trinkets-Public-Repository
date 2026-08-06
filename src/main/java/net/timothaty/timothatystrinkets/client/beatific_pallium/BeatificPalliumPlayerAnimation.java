package net.timothaty.timothatystrinkets.client.beatific_pallium;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.animation.PlayerCastAnimationConflicts;
import net.timothaty.timothatystrinkets.client.animation.PlayerCastHandDustVisuals;
import net.timothaty.timothatystrinkets.client.animation.PlayerCastHandDustVisuals.CastProfile;
import net.timothaty.timothatystrinkets.client.cherubims_wisdom.CherubimsWisdomPlayerAnimation;
import net.timothaty.timothatystrinkets.client.compat.FirstPersonModelCompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
		modid = TimothatysTrinketsMod.MODID,
		value = Dist.CLIENT,
		bus = EventBusSubscriber.Bus.MOD
)
public final class BeatificPalliumPlayerAnimation {
	public static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"beatific_pallium_cast"
	);
	public static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"beatific_pallium_cast"
	);
	public static final int LAYER_PRIORITY = 1601;

	private static final int FIRST_PERSON_TRANSITION_TICKS = 2;
	private static final FirstPersonConfiguration FIRST_PERSON_CONFIGURATION =
			new FirstPersonConfiguration()
					.setShowRightArm(true)
					.setShowLeftArm(true)
					.setShowRightItem(true)
					.setShowLeftItem(true)
					.setShowArmor(false);
	private static boolean registered;
	private static boolean warnedMissingController;
	private static boolean warnedMissingAnimation;

	private BeatificPalliumPlayerAnimation() {
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(BeatificPalliumPlayerAnimation::register);
	}

	public static boolean start(int casterEntityId) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || casterEntityId < 0)
			return false;

		Entity entity = level.getEntity(casterEntityId);
		if (!(entity instanceof AbstractClientPlayer player))
			return false;

		PlayerAnimationController controller = getController(player);
		if (controller == null) {
			warnMissingController(player);
			return false;
		}

		if (PlayerCastAnimationConflicts.hasVisualConflict(player)
				|| CherubimsWisdomPlayerAnimation.isActive(player)) {
			stop(controller);
			PlayerCastHandDustVisuals.stop(casterEntityId, CastProfile.BEATIFIC_PALLIUM);
			return false;
		}

		if (!controller.triggerAnimation(ANIMATION_ID)) {
			if (!warnedMissingAnimation) {
				warnedMissingAnimation = true;
				TimothatysTrinketsMod.LOGGER.warn(
						"PAL animation resource {} was not found for Beatific Pallium caster {}",
						ANIMATION_ID,
						player.getGameProfile().getName()
				);
			}
			return false;
		}
		return true;
	}

	public static boolean isActive(AbstractClientPlayer player) {
		PlayerAnimationController controller = getController(player);
		return controller != null && controller.isActive();
	}

	public static void clear() {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level != null) {
			for (AbstractClientPlayer player : level.players())
				stop(player);
		} else if (minecraft.player != null) {
			stop(minecraft.player);
		}
	}

	private static void register() {
		if (registered)
			return;

		registered = true;
		PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
				LAYER_ID,
				LAYER_PRIORITY,
				BeatificPalliumPlayerAnimation::createController
		);
		NeoForge.EVENT_BUS.addListener(BeatificPalliumPlayerAnimation::onClientTick);
	}

	private static PlayerAnimationController createController(AbstractClientPlayer player) {
		PlayerAnimationController controller = new PlayerAnimationController(
				player,
				(animationController, animationData, animationSetter) -> PlayState.STOP
		);
		controller.setFirstPersonModeHandler(animationController ->
				FirstPersonModelCompat.isTrueFirstPersonActive()
						? FirstPersonMode.DISABLED
						: FirstPersonMode.THIRD_PERSON_MODEL
		);
		controller.setFirstPersonConfiguration(FIRST_PERSON_CONFIGURATION);
		controller.setFirstPersonFollowsCamera(true);
		controller.setFirstPersonTransitionLength(FIRST_PERSON_TRANSITION_TICKS);
		return controller;
	}

	private static void onClientTick(ClientTickEvent.Post event) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return;

		for (AbstractClientPlayer player : level.players()) {
			PlayerAnimationController controller = getController(player);
			if (controller != null && controller.isActive()
					&& (PlayerCastAnimationConflicts.hasVisualConflict(player)
					|| CherubimsWisdomPlayerAnimation.isActive(player))) {
				stop(controller);
				PlayerCastHandDustVisuals.stop(player.getId(), CastProfile.BEATIFIC_PALLIUM);
			}
		}
	}

	private static PlayerAnimationController getController(AbstractClientPlayer player) {
		IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER_ID);
		return layer instanceof PlayerAnimationController controller ? controller : null;
	}

	private static void warnMissingController(AbstractClientPlayer player) {
		if (warnedMissingController)
			return;
		warnedMissingController = true;
		TimothatysTrinketsMod.LOGGER.warn(
				"PAL controller {} is unavailable for Beatific Pallium caster {}",
				LAYER_ID,
				player.getGameProfile().getName()
		);
	}

	private static void stop(AbstractClientPlayer player) {
		PlayerAnimationController controller = getController(player);
		if (controller != null)
			stop(controller);
	}

	private static void stop(PlayerAnimationController controller) {
		controller.stopTriggeredAnimation();
		controller.stop();
	}
}
