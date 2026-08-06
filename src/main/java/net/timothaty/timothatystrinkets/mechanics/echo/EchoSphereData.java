package net.timothaty.timothatystrinkets.mechanics.echo;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public final class EchoSphereData {
	public static final ResourceKey<Biome> DEEP_DARK_BIOME = ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("deep_dark"));

	public static final String NBT_UNTIL = "ttr_resonance_cage_until";
	public static final String NBT_X = "ttr_resonance_cage_x";
	public static final String NBT_Y = "ttr_resonance_cage_y";
	public static final String NBT_Z = "ttr_resonance_cage_z";
	public static final String NBT_YROT = "ttr_resonance_cage_yrot";
	public static final String NBT_XROT = "ttr_resonance_cage_xrot";
	public static final String NBT_HIDE_AT = "ttr_resonance_cage_hide_at";
	public static final String NBT_OLD_INVISIBLE = "ttr_resonance_cage_old_invisible";
	public static final String NBT_OLD_INVULNERABLE = "ttr_resonance_cage_old_invulnerable";
	public static final String NBT_OLD_NO_GRAVITY = "ttr_resonance_cage_old_no_gravity";
	public static final String NBT_OLD_NO_PHYSICS = "ttr_resonance_cage_old_no_physics";
	public static final String NBT_OLD_NO_AI = "ttr_resonance_cage_old_no_ai";
	public static final String NBT_DEEP_DARK = "ttr_resonance_cage_deep_dark";
	public static final String NBT_CASTER_UUID = "ttr_resonance_cage_caster_uuid";

	public static final int CAGE_DURATION_TICKS = 45;
	public static final int ECHO_SPHERE_COOLDOWN_TICKS = 20 * 23;
	public static final int MODEL_HIDE_DELAY_TICKS = 3;
	public static final int KILL_REGENERATION_TICKS = 10 * 20;
	public static final double INTANGIBLE_BOX_HALF_SIZE = 1.0E-4D;
	public static final float RELEASE_DAMAGE = 6.0F;
	public static final float DEEP_DARK_RELEASE_DAMAGE_MULTIPLIER = 2F;

	public static final ResourceLocation ECHO_SPHERE_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "echo_sphere");

	private EchoSphereData() {
	}
}
