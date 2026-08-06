package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.client.animation.PlayerCastHandAnchorTracker;
import net.timothaty.timothatystrinkets.client.animation.PlayerCastHandDustVisuals;
import net.timothaty.timothatystrinkets.client.gorge.GorgeAnimationState;
import net.timothaty.timothatystrinkets.client.gorge.GorgeCameraShakeHandler;
import net.timothaty.timothatystrinkets.client.gorge.GorgeFirstPersonAnimation;
import net.timothaty.timothatystrinkets.client.beatific_pallium.BeatificPalliumPlayerAnimation;
import net.timothaty.timothatystrinkets.client.cherubims_wisdom.CherubimsWisdomPlayerAnimation;
import net.timothaty.timothatystrinkets.client.cherubims_wisdom.CherubimsWisdomActivationVisuals;
import net.timothaty.timothatystrinkets.client.hubris.HubrisActivationClientState;
import net.timothaty.timothatystrinkets.client.debtlord.DebtlordHoldClientState;
import net.timothaty.timothatystrinkets.client.hubris.HubrisClientState;
import net.timothaty.timothatystrinkets.client.hubris.PlayerHandAnchorTracker;
import net.timothaty.timothatystrinkets.client.handler.ConcussiveStrikeCameraShakeHandler;
import net.timothaty.timothatystrinkets.client.morgenshtern.MorgenshternCameraShakeHandler;
import net.timothaty.timothatystrinkets.client.morgenshtern.MorgenshternDecapitationClientState;
import net.timothaty.timothatystrinkets.client.morgenshtern.MorgenshternDecapitationRenderer;
import net.timothaty.timothatystrinkets.client.morgenshtern.MorgenshternStrikeClientState;
import net.timothaty.timothatystrinkets.client.particle.DesolationParticle;
import net.timothaty.timothatystrinkets.client.particle.HealingPresenceAuraParticle;
import net.timothaty.timothatystrinkets.client.particle.MoltenBaneMarkParticle;
import net.timothaty.timothatystrinkets.client.particle.StaggerSpiralParticle;
import net.timothaty.timothatystrinkets.client.particle.StunnedSpiralParticle;
import net.timothaty.timothatystrinkets.client.particle.VoidMarkParticle;
import net.timothaty.timothatystrinkets.client.stunned.StunnedClientAnimationState;
import net.timothaty.timothatystrinkets.client.stunned.StunnedClientControl;
import net.timothaty.timothatystrinkets.client.wrath_of_the_wicked.WrathOfTheWickedClientState;
import net.timothaty.timothatystrinkets.client.vfx.soul_rip.SoulRipTrailHandler;
import net.timothaty.timothatystrinkets.client.vfx.spark.SparkTrailHandler;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class TransientClientStateCleanup {
	private TransientClientStateCleanup() {
	}

	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		MorgenshternStrikeClientState.clear();
		MorgenshternDecapitationClientState.clear();
		MorgenshternDecapitationRenderer.clearAndRestore();
		StunnedClientAnimationState.clear();
		StunnedClientControl.clear();
		ConcussiveStrikeCameraShakeHandler.clear();
		SparkTrailHandler.clear();
		SoulRipTrailHandler.clear();
		DesolationParticle.clearTrackedParticles();
		HealingPresenceAuraParticle.clearTrackedParticles();
		MoltenBaneMarkParticle.clearTrackedParticles();
		StaggerSpiralParticle.clearTrackedParticles();
		StunnedSpiralParticle.clearTrackedParticles();
		VoidMarkParticle.clearTrackedParticles();
		GorgeAnimationState.clear();
		GorgeFirstPersonAnimation.clear();
		BeatificPalliumPlayerAnimation.clear();
		CherubimsWisdomPlayerAnimation.clear();
		CherubimsWisdomActivationVisuals.clear();
		PlayerCastHandDustVisuals.clear();
		PlayerCastHandAnchorTracker.clear();
		GorgeCameraShakeHandler.clear();
		HubrisActivationClientState.clear();
		DebtlordHoldClientState.clear();
		HubrisClientState.clear();
		PlayerHandAnchorTracker.clear();
		WrathOfTheWickedClientState.clear();
		MorgenshternCameraShakeHandler.clear();
	}
}
