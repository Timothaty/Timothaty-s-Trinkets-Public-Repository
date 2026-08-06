package net.timothaty.timothatystrinkets.mechanics.flaming_ember;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public final class FlamingEmberTags {
	public static final TagKey<Block> HEAT_SOURCES = blockTag("flaming_ember_heat_sources");
	public static final TagKey<EntityType<?>> HEAT_MOBS = entityTypeTag("flaming_ember_heat_mobs");
	public static final TagKey<Biome> HOT_BIOMES = biomeTag("flaming_ember_hot_biomes");
	public static final TagKey<Biome> COLD_BIOMES = biomeTag("flaming_ember_cold_biomes");

	private FlamingEmberTags() {
	}

	private static TagKey<Block> blockTag(String path) {
		return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, path));
	}

	private static TagKey<EntityType<?>> entityTypeTag(String path) {
		return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, path));
	}

	private static TagKey<Biome> biomeTag(String path) {
		return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, path));
	}
}
