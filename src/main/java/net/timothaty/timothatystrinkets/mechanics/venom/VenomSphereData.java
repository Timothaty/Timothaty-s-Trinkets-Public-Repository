package net.timothaty.timothatystrinkets.mechanics.venom;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.resources.ResourceLocation;

public final class VenomSphereData {
	public static final ResourceLocation VENOM_SPHERE_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "venom_sphere");

	public static final int MAX_STACKS = 5;
	public static final int HITS_PER_STACK = 2;
	public static final int MAX_ACTIVE_TARGETS = 5;
	public static final int EFFECT_DURATION_TICKS = 20 * 4;

	public static final double ARMOR_REDUCTION_PER_STACK = -1.0D;
	public static final double SPEED_REDUCTION_PER_STACK = -0.025D;

	public static final String NBT_ACTIVE_TARGETS = "TimothatysTrinketsVenomSphereActiveTargets";
	public static final String NBT_TARGET_UUID = "TargetUuid";
	public static final String NBT_TARGET_EXPIRE_TICK = "ExpireTick";

	public static final String NBT_STACK_OWNER_UUID = "TimothatysTrinketsVenomSphereStackOwnerUuid";
	public static final String NBT_HITS = "TimothatysTrinketsVenomSphereHits";
	public static final String NBT_STACKS = "TimothatysTrinketsVenomSphereStacks";
	public static final String NBT_EXPIRE_TICK = "TimothatysTrinketsVenomSphereExpireTick";

	private VenomSphereData() {
	}
}
