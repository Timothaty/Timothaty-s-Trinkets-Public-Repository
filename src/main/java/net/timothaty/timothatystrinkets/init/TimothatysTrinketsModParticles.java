package net.timothaty.timothatystrinkets.init;

import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.timothaty.timothatystrinkets.client.particle.*;

@EventBusSubscriber(Dist.CLIENT)
public class TimothatysTrinketsModParticles {
	@SubscribeEvent
	public static void registerParticles(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.CLEANSING_DUST_PARTICLE.get(), CleansingDustParticleParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.CLEANSING_DUST_PARTICLE_UP.get(), CleansingDustParticleUpParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.DRUM_BEAT.get(), DrumBeatParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.BLIGHTED_DUST.get(), BlightedDustParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.BLOOD_BIT.get(), BloodBitParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.SACRIFICE_SUCCES.get(), SacrificeSuccesParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.SACRIFICE_FAILED.get(), SacrificeFailedParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.DEAD_LUCK.get(), DeadLuckParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.UNDEADIFICATION_PARTICLE_VFX.get(), UndeadificationParticleVFXParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.ROTTEN_CHUNK.get(), RottenChunkParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.DESOLATION.get(), DesolationParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.HEALING_PRESENCE_AURA.get(), HealingPresenceAuraParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.VOID_MARK.get(), VoidMarkParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.RESONANCE_BOTTOM_TOP.get(), ResonanceBottomTopParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.MOLTEN_BANE_MARK.get(), MoltenBaneMarkParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.FIERY_SWEEP_PARTICLE.get(), FierySweepParticleParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.PRIDEFUL_SWEEP.get(), PridefulSweepParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.MORGENSHTERN_SWEEP.get(), MorgenshternSweepParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.STUNNED_SPIRAL.get(), StunnedSpiralParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.SHOCKWAVE.get(), ShockwaveParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.WICKED_PULSE.get(), WickedPulseParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.STAGGER_SPIRAL.get(), StaggerSpiralParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.TOKSIK.get(), ToksikParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.BIOME_ENERGY.get(), BiomeEnergyParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.RUNIC_FERTILIZER.get(), RunicFertilizerParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.EARTH_IMPACT.get(), EarthImpactParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.ROAR_OF_FEAR.get(), RoarOfFearParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.DEBTLORD_SUMMON.get(), DebtlordSummonParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.DOT.get(), DotParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.CHERUBIMS_WISDOM_EXPERIENCE_DOT.get(), CherubimsWisdomExperienceDotParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.BABAH.get(), BabahParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.EMBER_IMPULSE.get(), EmberImpulseParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.SPARK.get(), SparkParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.TINTED_SHARD.get(), TintedShardParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.BEATIFIC_PALLIUM_EXPLOSION_RING.get(), BeatificPalliumExplosionRingParticle::provider);
		event.registerSpriteSet(TimothatysTrinketsModParticleTypes.RITUAL_SMOKE.get(), RitualSmokeParticle::provider);
	}
}
