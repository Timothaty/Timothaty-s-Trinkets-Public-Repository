package net.timothaty.timothatystrinkets.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

public class TimothatysTrinketsModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, TimothatysTrinketsMod.MODID);
	public static final DeferredHolder<SoundEvent, SoundEvent> PAGAMS_CHARM_DROP = REGISTRY.register("pagams_charm_drop",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "pagams_charm_drop")));
	public static final DeferredHolder<SoundEvent, SoundEvent> EQUIP_UNDEAD_KNIGHT_ARMLET = REGISTRY.register("equip_undead_knight_armlet",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "equip_undead_knight_armlet")));
	public static final DeferredHolder<SoundEvent, SoundEvent> MAGICAL_HIT_PROC = REGISTRY.register("magical_hit_proc",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "magical_hit_proc")));
	public static final DeferredHolder<SoundEvent, SoundEvent> CLEANSING_DUST_USE = REGISTRY.register("cleansing_dust_use",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "cleansing_dust_use")));
	public static final DeferredHolder<SoundEvent, SoundEvent> FARMERS_RING_EQUIP = REGISTRY.register("farmers_ring_equip",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "farmers_ring_equip")));
	public static final DeferredHolder<SoundEvent, SoundEvent> CHAMPIONS_GAUNTLET_CRIT_PROC = REGISTRY.register("champions_gauntlet_crit_proc",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "champions_gauntlet_crit_proc")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DRUM_STACKING = REGISTRY.register("drum_stacking",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "drum_stacking")));
	public static final DeferredHolder<SoundEvent, SoundEvent> INDULGENCY_USED = REGISTRY.register("indulgency_used",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "indulgency_used")));
	public static final DeferredHolder<SoundEvent, SoundEvent> RARE_ITEM_SACRIFICE = REGISTRY.register("rare_item_sacrifice",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "rare_item_sacrifice")));
	public static final DeferredHolder<SoundEvent, SoundEvent> COMMON_ITEM_SACRIFICE = REGISTRY.register("common_item_sacrifice",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "common_item_sacrifice")));
	public static final DeferredHolder<SoundEvent, SoundEvent> EQUIP_RITUAL_DAGGER = REGISTRY.register("equip_ritual_dagger",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "equip_ritual_dagger")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DEAD_LUCK_DEATH = REGISTRY.register("dead_luck_death",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "dead_luck_death")));
	public static final DeferredHolder<SoundEvent, SoundEvent> PURGE = REGISTRY.register("purge",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "purge")));
	public static final DeferredHolder<SoundEvent, SoundEvent> LAND_BLIGHTED = REGISTRY.register("land_blighted",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "land_blighted")));
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEADIFICATION_START = REGISTRY.register("undeadification_start",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "undeadification_start")));
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEADIFICATION_LOOP = REGISTRY.register("undeadification_loop",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "undeadification_loop")));
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEADIFICATION_SUCCESFUL = REGISTRY.register("undeadification_succesful",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "undeadification_succesful")));
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEADIFICATION_FAILED = REGISTRY.register("undeadification_failed",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "undeadification_failed")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DESOLATED = REGISTRY.register("desolated",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "desolated")));
	public static final DeferredHolder<SoundEvent, SoundEvent> MARKED_BY_VOID_APPLIED = REGISTRY.register("marked_by_void_applied",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "marked_by_void_applied")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BOTTLE_OF_BLOOD_DRINK = REGISTRY.register("bottle_of_blood_drink",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "bottle_of_blood_drink")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DRUM_BEAT_WAVE_SOUND = REGISTRY.register("drum_beat_wave_sound",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "drum_beat_wave_sound")));
	public static final DeferredHolder<SoundEvent, SoundEvent> STUNNED_LOOP = REGISTRY.register("stunned_loop",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "stunned_loop")));
	public static final DeferredHolder<SoundEvent, SoundEvent> MOLTEN_BANE_MARK = REGISTRY.register("molten_bane_mark",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "molten_bane_mark")));
	public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_STACKING = REGISTRY.register("fire_stacking",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "fire_stacking")));
	public static final DeferredHolder<SoundEvent, SoundEvent> STRIKER_EQUIP = REGISTRY.register("striker_equip",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "striker_equip")));
	public static final DeferredHolder<SoundEvent, SoundEvent> RARE_ITEM_DROP_VILLAGER = REGISTRY.register("rare_item_drop_villager",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "rare_item_drop_villager")));
	public static final DeferredHolder<SoundEvent, SoundEvent> SACRAMENT_STAGE_COMPLETED = REGISTRY.register("sacrament_stage_completed",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "sacrament_stage_completed")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DEED_ACCOMPLISHED = REGISTRY.register("deed_accomplished",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "deed_accomplished")));
	public static final DeferredHolder<SoundEvent, SoundEvent> SACRAMENT_MOB_SLAYED = REGISTRY.register("sacrament_mob_slayed",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "sacrament_mob_slayed")));
	public static final DeferredHolder<SoundEvent, SoundEvent> EQUIP_FIRE_ORB = REGISTRY.register("equip_fire_orb",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "equip_fire_orb")));
	public static final DeferredHolder<SoundEvent, SoundEvent> EQUIP_VOID_SPHERE = REGISTRY.register("equip_void_sphere",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "equip_void_sphere")));
	public static final DeferredHolder<SoundEvent, SoundEvent> EQUIP_VENOM_SPHERE = REGISTRY.register("equip_venom_sphere",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "equip_venom_sphere")));
	public static final DeferredHolder<SoundEvent, SoundEvent> EQUIP_ECHO_SPHERE = REGISTRY.register("equip_echo_sphere", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "equip_echo_sphere")));
	public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_ORB_BLOCK_LOOP = REGISTRY.register("echo_orb_block_loop", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "echo_orb_block_loop")));
	public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_SPHERE_TRANSMUTATION = REGISTRY.register("echo_sphere_transmutation", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "echo_sphere_transmutation")));
	public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_ORB_CLAIM = REGISTRY.register("echo_orb_claim", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "echo_orb_claim")));
	public static final DeferredHolder<SoundEvent, SoundEvent> ARMORED_TOXICITY_HIT = REGISTRY.register("armored_toxicity_hit",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "armored_toxicity_hit")));
	public static final DeferredHolder<SoundEvent, SoundEvent> NON_ARMORED_TOXICITY_HIT = REGISTRY.register("non_armored_toxicity_hit",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "non_armored_toxicity_hit")));
	public static final DeferredHolder<SoundEvent, SoundEvent> TOXIC_DROPLET = REGISTRY.register("toxic_droplet", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "toxic_droplet")));
	public static final DeferredHolder<SoundEvent, SoundEvent> CONCUSSIVE_STRIKE = REGISTRY.register("concussive_strike",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "concussive_strike")));
	public static final DeferredHolder<SoundEvent, SoundEvent> NECRO_CAST_UNDEADIFICATION = REGISTRY.register("necro_cast_undeadification",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "necro_cast_undeadification")));
	public static final DeferredHolder<SoundEvent, SoundEvent> NECRO_CAST_MAGIC = REGISTRY.register("necro_cast_magic",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "necro_cast_magic")));
	public static final DeferredHolder<SoundEvent, SoundEvent> NECROMANCER_HURT = REGISTRY.register("necromancer_hurt",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "necromancer_hurt")));
	public static final DeferredHolder<SoundEvent, SoundEvent> FEAR = REGISTRY.register("fear", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "fear")));
	public static final DeferredHolder<SoundEvent, SoundEvent> HOOF_STOMP = REGISTRY.register("hoof_stomp", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "hoof_stomp")));
	public static final DeferredHolder<SoundEvent, SoundEvent> STOMP_PREPARATION = REGISTRY.register("stomp_preparation",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "stomp_preparation")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DEBTLORD_ROAR = REGISTRY.register("debtlord_roar",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "debtlord_roar")));
	public static final DeferredHolder<SoundEvent, SoundEvent> HEAVY_SWING = REGISTRY.register("heavy_swing",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "heavy_swing")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DEBTLORD_DISARM = REGISTRY.register("debtlord_disarm",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "debtlord_disarm")));
	public static final DeferredHolder<SoundEvent, SoundEvent> HORN_HIT = REGISTRY.register("horn_hit", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "horn_hit")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DEBTLORD_DEATH = REGISTRY.register("debtlord_death",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "debtlord_death")));
	public static final DeferredHolder<SoundEvent, SoundEvent> FEAR_MY_CLAWS = REGISTRY.register("fear_my_claws",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "fear_my_claws")));
	public static final DeferredHolder<SoundEvent, SoundEvent> HOOF_STEP = REGISTRY.register("hoof_step", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "hoof_step")));
	public static final DeferredHolder<SoundEvent, SoundEvent> CHAINS_LAUNCH = REGISTRY.register("chains_launch",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "chains_launch")));
	public static final DeferredHolder<SoundEvent, SoundEvent> CHAINS_CAUGHT = REGISTRY.register("chains_caught",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "chains_caught")));
	public static final DeferredHolder<SoundEvent, SoundEvent> REFRESHING_CHALICE_USE = REGISTRY.register("refreshing_chalice_use",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "refreshing_chalice_use")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DEBTLORD_LASER = REGISTRY.register("debtlord_laser", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "debtlord_laser")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BLOOD_BOIL = REGISTRY.register("blood_boil", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "blood_boil")));
	public static final DeferredHolder<SoundEvent, SoundEvent> EXTORTION_SUCCESS = REGISTRY.register("extortion_success", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "extortion_success")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BLOODSTAINED = REGISTRY.register("bloodstained", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "bloodstained")));
	public static final DeferredHolder<SoundEvent, SoundEvent> ALTAR_PUNISH = REGISTRY.register("altar_punish", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "altar_punish")));
	public static final DeferredHolder<SoundEvent, SoundEvent> ALTAR_SHOT = REGISTRY.register("altar_shot", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "altar_shot")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DLRD_TALK = REGISTRY.register("dlrd_talk", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "dlrd_talk")));
	public static final DeferredHolder<SoundEvent, SoundEvent> SWORD_PARRY = REGISTRY.register("sword_parry", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "sword_parry")));
	public static final DeferredHolder<SoundEvent, SoundEvent> EMBER_IMPULSE = REGISTRY.register("ember_impulse", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "ember_impulse")));
	public static final DeferredHolder<SoundEvent, SoundEvent> GORGE_EAT = REGISTRY.register("gorge_eat",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "gorge_eat")));
	public static final DeferredHolder<SoundEvent, SoundEvent> HUBRIS_ACTIVATION = REGISTRY.register("hubris_activation",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "hubris_activation")));
	public static final DeferredHolder<SoundEvent, SoundEvent> HUBRIS_ACTIVATION_MACE = REGISTRY.register("hubris_activation_mace",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "hubris_activation_mace")));
	public static final DeferredHolder<SoundEvent, SoundEvent> CHERUBIMS_WISDOM = REGISTRY.register("cherubims_wisdom",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "cherubims_wisdom")));
	public static final DeferredHolder<SoundEvent, SoundEvent> CHERUBIMS_WISDOM_HIT = REGISTRY.register("cherubims_wisdom_hit",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "cherubims_wisdom_hit")));
	public static final DeferredHolder<SoundEvent, SoundEvent> ANGELS_SHROUD_ACTIVATION = REGISTRY.register("angels_shroud_activation",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "angels_shroud_activation")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BEATIFIC_PALLIUM_CAST = REGISTRY.register("beatific_pallium_cast",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "beatific_pallium_cast")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BEATIFIC_PALLIUM_EXPLOSION = REGISTRY.register("beatific_pallium_explosion",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "beatific_pallium_explosion")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BEATIFIC_PALLIUM_LOOP = REGISTRY.register("beatific_pallium_loop",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "beatific_pallium_loop")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BEATIFIC_PALLIUM_IMPACT = REGISTRY.register("beatific_pallium_impact",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "beatific_pallium_impact")));
	public static final DeferredHolder<SoundEvent, SoundEvent> EMBER_BLAZE_POWDER = REGISTRY.register("ember_blaze_powder",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "ember_blaze_powder")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BEAD_INSERT = REGISTRY.register("bead_insert", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "bead_insert")));
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_KNIGHT_STEP = REGISTRY.register("undead_knight_step",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "undead_knight_step")));
	public static final DeferredHolder<SoundEvent, SoundEvent> LEATHER_PARRY_DIRECTION = REGISTRY.register("leather_parry_direction",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "leather_parry_direction")));
	public static final DeferredHolder<SoundEvent, SoundEvent> METALLIC_PARRY_DIRECTION = REGISTRY.register("metallic_parry_direction",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "metallic_parry_direction")));
	public static final DeferredHolder<SoundEvent, SoundEvent> COUNTER_ATTACK = REGISTRY.register("counter_attack", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "counter_attack")));
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_KNIGHT_DEATH_1 = REGISTRY.register("undead_knight_death_1",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "undead_knight_death_1")));
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_KNIGHT_DEATH_2 = REGISTRY.register("undead_knight_death_2",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "undead_knight_death_2")));
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_KNIGHT_DEATH_3 = REGISTRY.register("undead_knight_death_3",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "undead_knight_death_3")));
	public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_COLLECT = REGISTRY.register("soul_collect", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "soul_collect")));
	public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_DESTRUCTION = REGISTRY.register("soul_destruction", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "soul_destruction")));
	public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_ABSORPTION_CAST_LOOP = REGISTRY.register("soul_absorption_cast_loop",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "soul_absorption_cast_loop")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BOTTLE_SOUL_ORB_CATCH = REGISTRY.register("bottle_soul_orb_catch",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "bottle_soul_orb_catch")));
	public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_EMPOWER_HOLD_LOOP = REGISTRY.register("soul_empower_hold_loop",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "soul_empower_hold_loop")));
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_KNIGHT_REINCARNATION_1 = REGISTRY.register("undead_knight_reincarnation_1",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "undead_knight_reincarnation_1")));
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_KNIGHT_REINCARNATION_2 = REGISTRY.register("undead_knight_reincarnation_2",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "undead_knight_reincarnation_2")));
	public static final DeferredHolder<SoundEvent, SoundEvent> UNDEAD_KNIGHT_REINCARNATION_3 = REGISTRY.register("undead_knight_reincarnation_3",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "undead_knight_reincarnation_3")));
	public static final DeferredHolder<SoundEvent, SoundEvent> MACE_LEATHER_KILL = REGISTRY.register("mace_leather_kill",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "mace_leather_kill")));
	public static final DeferredHolder<SoundEvent, SoundEvent> MACE_CHAINMAIL_KILL = REGISTRY.register("mace_chainmail_kill",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "mace_chainmail_kill")));
	public static final DeferredHolder<SoundEvent, SoundEvent> MACE_PLATE_KILL = REGISTRY.register("mace_plate_kill",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "mace_plate_kill")));
	public static final DeferredHolder<SoundEvent, SoundEvent> MACE_FLESH_KILL = REGISTRY.register("mace_flesh_kill",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "mace_flesh_kill")));
	public static final DeferredHolder<SoundEvent, SoundEvent> MACE_SKELETON_KILL = REGISTRY.register("mace_skeleton_kill",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("timothatys_trinkets", "mace_skeleton_kill")));
}
