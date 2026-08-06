package net.timothaty.timothatystrinkets.mechanics.pagans_charm;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class PaganCharmTags {
	public static final TagKey<Biome> PAGAN_BIOMES = biomeTag("pagan_biomes");
	public static final TagKey<Biome> UNIQUE_PAGAN_BIOMES = biomeTag("unique_pagan_biomes");
	public static final TagKey<Biome> NETHER_BIOMES = vanillaBiomeTag("is_nether");

	private PaganCharmTags() {
	}

	private static TagKey<Biome> biomeTag(String path) {
		return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, path));
	}

	private static TagKey<Biome> vanillaBiomeTag(String path) {
		return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", path));
	}
}
