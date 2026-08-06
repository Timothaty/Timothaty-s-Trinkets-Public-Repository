package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class HubrisData {
	public static final int ACTIVATION_TICKS = 29;
	public static final int SOUND_TICK = 2;
	public static final int STUN_IMMUNITY_TICKS = 60;
	public static final int DURATION_TICKS = 40 * 20;
	public static final int VISUAL_KEEPALIVE_INTERVAL_TICKS = 20;
	public static final int COOLDOWN_TICKS = 45 * 20;
	public static final int SLOWNESS_DURATION_TICKS = 5 * 20;
	public static final int INITIAL_THORNS = 4;
	public static final float TARGET_HEALTH_THRESHOLD = 1.2F;
	public static final float MIN_ATTACK_STRENGTH = 0.9F;
	public static final float SOUL_DAMAGE_RATIO = 0.30F;
	public static final float BLOCKED_SOUL_DAMAGE_RATIO = 0.2F;
	public static final double MAX_HEALTH_PENALTY = -4.0D;
	private static final float[] STRIKE_MULTIPLIERS = {1.05F, 1.10F, 1.15F, 1.20F};
	public static final float CRIMSON_RED = 165.0F / 255.0F;
	public static final float CRIMSON_GREEN = 10.0F / 255.0F;
	public static final float CRIMSON_BLUE = 44.0F / 255.0F;

	public static final ResourceLocation CAST_LOCK_ID = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"hubris_activation"
	);
	public static final ResourceLocation MAX_HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
			TimothatysTrinketsMod.MODID,
			"hubris_max_health_penalty"
	);
	public static final TagKey<Item> HEAVY_ARMS = TagKey.create(
			Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "heavy_arms")
	);

	private HubrisData() {
	}

	public static float strikeMultiplier(int strikeIndex) {
		return STRIKE_MULTIPLIERS[Math.max(0, Math.min(STRIKE_MULTIPLIERS.length - 1, strikeIndex))];
	}
}
