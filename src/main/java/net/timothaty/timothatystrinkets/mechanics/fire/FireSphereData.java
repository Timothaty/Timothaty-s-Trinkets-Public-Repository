package net.timothaty.timothatystrinkets.mechanics.fire;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.resources.ResourceLocation;

public final class FireSphereData {
	public static final ResourceLocation FIRE_SPHERE_ID = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "fire_sphere");

	public static final String NBT_TARGET_ID = "TimothatysTrinketsFireSphereTargetId";
	public static final String NBT_STACKS = "TimothatysTrinketsFireSphereStacks";
	public static final String NBT_EXPIRE_TICK = "TimothatysTrinketsFireSphereExpireTick";
	public static final String NBT_STACK_OWNER_UUID = "TimothatysTrinketsFireSphereStackOwnerUuid";
	public static final String NBT_LAST_PROC_TARGET_ID = "TimothatysTrinketsFireSphereLastProcTargetId";
	public static final String NBT_LAST_PROC_EXPIRE_TICK = "TimothatysTrinketsFireSphereLastProcExpireTick";
	public static final String NBT_MOLTEN_BANE_END_HANDLED = "TimothatysTrinketsMoltenBaneEndHandled";

	public static final int MAX_STACKS = 4;
	public static final int STACK_EXPIRE_TICKS = 20 * 3;
	public static final int MOLTEN_BANE_DURATION_TICKS = 20 * 6;
	public static final int FIRE_SPHERE_COOLDOWN_TICKS = 20 * 15;
	public static final float MOLTEN_BANE_MAGIC_DAMAGE = 4.0F;

	private FireSphereData() {
	}
}
