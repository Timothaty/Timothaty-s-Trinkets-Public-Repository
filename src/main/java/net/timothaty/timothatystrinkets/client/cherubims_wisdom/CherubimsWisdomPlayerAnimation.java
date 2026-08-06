package net.timothaty.timothatystrinkets.client.cherubims_wisdom;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.MirrorModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.animation.PlayerCastAnimationConflicts;
import net.timothaty.timothatystrinkets.client.beatific_pallium.BeatificPalliumPlayerAnimation;
import net.timothaty.timothatystrinkets.client.compat.FirstPersonModelCompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
		modid = TimothatysTrinketsMod.MODID,
		value = Dist.CLIENT,
		bus = EventBusSubscriber.Bus.MOD
)
public final class CherubimsWisdomPlayerAnimation {
	public static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"cherubims_wisdom"
	);
	public static final ResourceLocation ANIMATION_ID = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"cherubims_wisdom"
	);
	public static final int LAYER_PRIORITY = 1600;

	private static final int FIRST_PERSON_TRANSITION_TICKS = 2;
	private static final FirstPersonConfiguration FIRST_PERSON_CONFIGURATION =
			new FirstPersonConfiguration()
					.setShowRightArm(true)
					.setShowLeftArm(true)
					.setShowRightItem(true)
					.setShowLeftItem(true)
					.setShowArmor(false);
	private static boolean registered;

	private CherubimsWisdomPlayerAnimation() {
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(CherubimsWisdomPlayerAnimation::register);
	}

	public static boolean start(int entityId, int castingArmOrdinal) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || entityId < 0)
			return false;

		Entity entity = level.getEntity(entityId);
		if (!(entity instanceof AbstractClientPlayer player))
			return false;

		PlayerAnimationController controller = getController(player);
		if (controller == null)
			return false;

		if (PlayerCastAnimationConflicts.hasVisualConflict(player)
				|| BeatificPalliumPlayerAnimation.isActive(player)) {
			stop(controller);
			CherubimsWisdomActivationVisuals.stop(entityId);
			return false;
		}

		HumanoidArm[] arms = HumanoidArm.values();
		HumanoidArm castingArm = arms[Mth.clamp(castingArmOrdinal, 0, arms.length - 1)];
		MirrorModifier mirror = getMirrorModifier(controller);
		if (mirror != null)
			mirror.enabled = castingArm == HumanoidArm.LEFT;

		if (!controller.triggerAnimation(ANIMATION_ID)) {
			TimothatysTrinketsMod.LOGGER.warn(
					"PAL animation {} was not found for player {}",
					ANIMATION_ID,
					player.getGameProfile().getName()
			);
			return false;
		}
		return true;
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

	public static boolean isActive(AbstractClientPlayer player) {
		PlayerAnimationController controller = getController(player);
		return controller != null && controller.isActive();
	}

	private static void register() {
		if (registered)
			return;

		registered = true;
		PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
				LAYER_ID,
				LAYER_PRIORITY,
				CherubimsWisdomPlayerAnimation::createController
		);
		NeoForge.EVENT_BUS.addListener(CherubimsWisdomPlayerAnimation::onClientTick);
	}

	private static PlayerAnimationController createController(AbstractClientPlayer player) {
		PlayerAnimationController controller = new PlayerAnimationController(
				player,
				(animationController, animationData, animationSetter) -> PlayState.STOP
		);
		MirrorModifier mirror = new MirrorModifier();
		mirror.enabled = false;
		controller.addModifierLast(mirror);
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
					|| BeatificPalliumPlayerAnimation.isActive(player))) {
				stop(controller);
				CherubimsWisdomActivationVisuals.stop(player.getId());
			}
		}
	}

	private static PlayerAnimationController getController(AbstractClientPlayer player) {
		IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER_ID);
		return layer instanceof PlayerAnimationController controller ? controller : null;
	}

	private static MirrorModifier getMirrorModifier(PlayerAnimationController controller) {
		for (AbstractModifier modifier : controller.getModifiers()) {
			if (modifier instanceof MirrorModifier mirror)
				return mirror;
		}
		return null;
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
