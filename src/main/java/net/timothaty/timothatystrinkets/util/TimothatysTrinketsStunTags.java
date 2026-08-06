package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class TimothatysTrinketsStunTags {
	public static final TagKey<Item> HEAVY_ARMS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "heavy_arms"));

	public static final TagKey<EntityType<?>> STUN_BOSSES = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "stun_bosses")
	);

	public static final TagKey<EntityType<?>> STUN_MINIBOSSES = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "stun_minibosses")
	);

	public static final TagKey<EntityType<?>> STUN_IMMUNE = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "stun_immune")
	);

	public static final TagKey<EntityType<?>> STAGGER_CREATURES = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "stagger_creatures")
	);

	private TimothatysTrinketsStunTags() {
	}
}
