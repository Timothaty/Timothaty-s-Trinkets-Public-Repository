package net.timothaty.timothatystrinkets.mechanics.blight;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class BlightImmunityHelper {
	private static final TagKey<EntityType<?>> BLIGHT_IMMUNE = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "blight_immune")
	);

	private BlightImmunityHelper() {
	}

	public static boolean isBlightImmune(Entity entity) {
		return entity != null && entity.getType().is(BLIGHT_IMMUNE);
	}
}
