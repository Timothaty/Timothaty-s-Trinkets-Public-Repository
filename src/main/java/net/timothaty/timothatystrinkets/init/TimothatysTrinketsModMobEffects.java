package net.timothaty.timothatystrinkets.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.registries.Registries;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.gorge.GorgeMobEffect;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisMobEffect;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.angels_shroud.AngelsShroudMobEffect;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.beatific_pallium.BeatificPalliumMobEffect;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.cherubims_wisdom.CherubimsWisdomMobEffect;
import net.timothaty.timothatystrinkets.potion.*;

public class TimothatysTrinketsModMobEffects {
	public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(Registries.MOB_EFFECT, TimothatysTrinketsMod.MODID);
	public static final DeferredHolder<MobEffect, MobEffect> FEAR = REGISTRY.register("fear", () -> new FearMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> ANATHEMA = REGISTRY.register("anathema", () -> new AnathemaMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> BLOODSTAINED = REGISTRY.register("bloodstained", () -> new BloodstainedMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> PURGE = REGISTRY.register("purge", () -> new PurgeMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> HEALING_PRESENCE = REGISTRY.register("healing_presence", () -> new HealingPresenceMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> UNDEADIFICATION = REGISTRY.register("undeadification", () -> new UndeadificationMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> DESOLATED = REGISTRY.register("desolated", () -> new DesolatedMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> MARKED_BY_VOID = REGISTRY.register("marked_by_void", () -> new MarkedbyVoidMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> RESONANCE_CAGE = REGISTRY.register("resonance_cage", () -> new ResonanceCageMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> HAEMORRHAGE = REGISTRY.register("haemorrhage", () -> new HaemorrhageMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> MOLTEN_BANE = REGISTRY.register("molten_bane", () -> new MoltenBaneMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> STUNNED = REGISTRY.register("stunned", () -> new StunnedMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> STUN_IMMUNITY = REGISTRY.register("stun_immunity", () -> new StunImmunityMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> STAGGER = REGISTRY.register("stagger", () -> new StaggerMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> CORROSIVE_TOXICITY = REGISTRY.register("corrosive_toxicity", () -> new CorrosiveToxicityMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> SOUL_HUNGER = REGISTRY.register("soul_hunger", () -> new SoulHungerMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> UNHOLY_AURA = REGISTRY.register("unholy_aura", () -> new UnholyAuraMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> PUTREFACTION = REGISTRY.register("putrefaction", () -> new PutrefactionMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> NATURES_BARRIER = REGISTRY.register("natures_barrier", () -> new NaturesBarrierMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> SOUL_ABSORPTION = REGISTRY.register("soul_absorption", () -> new SoulAbsorptionMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> SOUL_EMPOWER = REGISTRY.register("soul_empower", () -> new SoulEmpowerMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> HAPPINESS = REGISTRY.register("happiness", () -> new HappinessMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> ALTARS_CURSE = REGISTRY.register("altars_curse", () -> new AltarsCurseMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> RIPOSTE = REGISTRY.register("riposte", () -> new RiposteMobEffect());
	public static final DeferredHolder<MobEffect, MobEffect> GORGE = REGISTRY.register("gorge", GorgeMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> HUBRIS = REGISTRY.register("hubris", HubrisMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> CHERUBIMS_WISDOM = REGISTRY.register("cherubims_wisdom", CherubimsWisdomMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> ANGELS_SHROUD = REGISTRY.register("angels_shroud", AngelsShroudMobEffect::new);
	public static final DeferredHolder<MobEffect, MobEffect> BEATIFIC_PALLIUM = REGISTRY.register("beatific_pallium", BeatificPalliumMobEffect::new);
}
