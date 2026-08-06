package net.timothaty.timothatystrinkets.mechanics.blight;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

public final class BlightSunShelterHelper {
	private static final TagKey<EntityType<?>> BLIGHT_SUN_SHELTER_SEEKERS = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "blight_sun_shelter_seekers")
	);

	private BlightSunShelterHelper() {
	}

	public static boolean shouldUseBlightSunShelter(Mob mob) {
		return mob != null
				&& mob.isInvertedHealAndHarm()
				&& mob.getType().is(BLIGHT_SUN_SHELTER_SEEKERS);
	}
}
