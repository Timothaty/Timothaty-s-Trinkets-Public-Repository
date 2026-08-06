package net.timothaty.timothatystrinkets.init;

import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.particle.BabahParticleOptions;
import net.timothaty.timothatystrinkets.particle.TintedShardParticleOptions;
import net.timothaty.timothatystrinkets.particle.RitualSmokeParticleOptions;

public class TimothatysTrinketsModParticleTypes {
	public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(Registries.PARTICLE_TYPE, TimothatysTrinketsMod.MODID);
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CLEANSING_DUST_PARTICLE = REGISTRY.register("cleansing_dust_particle", () -> new SimpleParticleType(true));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CLEANSING_DUST_PARTICLE_UP = REGISTRY.register("cleansing_dust_particle_up", () -> new SimpleParticleType(true));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRUM_BEAT = REGISTRY.register("drum_beat", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLIGHTED_DUST = REGISTRY.register("blighted_dust", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_BIT = REGISTRY.register("blood_bit", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SACRIFICE_SUCCES = REGISTRY.register("sacrifice_succes", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SACRIFICE_FAILED = REGISTRY.register("sacrifice_failed", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DEAD_LUCK = REGISTRY.register("dead_luck", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> UNDEADIFICATION_PARTICLE_VFX = REGISTRY.register("undeadification_particle_vfx", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROTTEN_CHUNK = REGISTRY.register("rotten_chunk", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DESOLATION = REGISTRY.register("desolation", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HEALING_PRESENCE_AURA = REGISTRY.register("healing_presence_aura", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOID_MARK = REGISTRY.register("void_mark", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RESONANCE_BOTTOM_TOP = REGISTRY.register("resonance_bottom_top", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MOLTEN_BANE_MARK = REGISTRY.register("molten_bane_mark", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIERY_SWEEP_PARTICLE = REGISTRY.register("fiery_sweep_particle", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PRIDEFUL_SWEEP = REGISTRY.register("prideful_sweep", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MORGENSHTERN_SWEEP = REGISTRY.register("morgenshtern_sweep", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STUNNED_SPIRAL = REGISTRY.register("stunned_spiral", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHOCKWAVE = REGISTRY.register("shockwave", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WICKED_PULSE = REGISTRY.register("wicked_pulse", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STAGGER_SPIRAL = REGISTRY.register("stagger_spiral", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TOKSIK = REGISTRY.register("toksik", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIOME_ENERGY = REGISTRY.register("biome_energy", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RUNIC_FERTILIZER = REGISTRY.register("runic_fertilizer", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EARTH_IMPACT = REGISTRY.register("earth_impact", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ROAR_OF_FEAR = REGISTRY.register("roar_of_fear", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DEBTLORD_SUMMON = REGISTRY.register("debtlord_summon", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DOT = REGISTRY.register("dot", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHERUBIMS_WISDOM_EXPERIENCE_DOT = REGISTRY.register("cherubims_wisdom_experience_dot", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, ParticleType<BabahParticleOptions>> BABAH = REGISTRY.register("babah", () -> new ParticleType<BabahParticleOptions>(false) {
		@Override
		public MapCodec<BabahParticleOptions> codec() {
			return BabahParticleOptions.CODEC;
		}

		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, BabahParticleOptions> streamCodec() {
			return BabahParticleOptions.STREAM_CODEC;
		}
	});
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EMBER_IMPULSE = REGISTRY.register("ember_impulse", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK = REGISTRY.register("spark", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, ParticleType<TintedShardParticleOptions>> TINTED_SHARD = REGISTRY.register(
			"tinted_shard", () -> new ParticleType<TintedShardParticleOptions>(false) {
				@Override
				public MapCodec<TintedShardParticleOptions> codec() {
					return TintedShardParticleOptions.CODEC;
				}

				@Override
				public StreamCodec<? super RegistryFriendlyByteBuf, TintedShardParticleOptions> streamCodec() {
					return TintedShardParticleOptions.STREAM_CODEC;
				}
			});
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BEATIFIC_PALLIUM_EXPLOSION_RING = REGISTRY.register(
			"beatific_pallium_explosion_ring", () -> new SimpleParticleType(false));
	public static final DeferredHolder<ParticleType<?>, ParticleType<RitualSmokeParticleOptions>> RITUAL_SMOKE = REGISTRY.register(
			"ritual_smoke", () -> new ParticleType<RitualSmokeParticleOptions>(false) {
				@Override
				public MapCodec<RitualSmokeParticleOptions> codec() {
					return RitualSmokeParticleOptions.CODEC;
				}

				@Override
				public StreamCodec<? super RegistryFriendlyByteBuf, RitualSmokeParticleOptions> streamCodec() {
					return RitualSmokeParticleOptions.STREAM_CODEC;
				}
			});
}
