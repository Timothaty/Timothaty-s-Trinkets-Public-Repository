package net.timothaty.timothatystrinkets.mechanics.morgenshtern;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class MorgenshternTags {
	public static final TagKey<EntityType<?>> EXPLICIT_HEADS = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(
					TimothatysTrinketsMod.MODID,
					"morgenshtern_explicit_heads"
			)
	);
	public static final TagKey<EntityType<?>> SKELETONS = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(
					TimothatysTrinketsMod.MODID,
					"morgenshtern_skeletons"
			)
	);
	public static final TagKey<EntityType<?>> PLATE_HEAD_ENTITIES =
			TagKey.create(
					Registries.ENTITY_TYPE,
					ResourceLocation.fromNamespaceAndPath(
							TimothatysTrinketsMod.MODID,
							"morgenshtern_plate_head_entities"
					)
			);
	public static final TagKey<EntityType<?>> CHAINMAIL_HEAD_ENTITIES =
			TagKey.create(
					Registries.ENTITY_TYPE,
					ResourceLocation.fromNamespaceAndPath(
							TimothatysTrinketsMod.MODID,
							"morgenshtern_chainmail_head_entities"
					)
			);
	public static final TagKey<Item> HELMET_BLACKLIST = TagKey.create(
			Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(
					TimothatysTrinketsMod.MODID,
					"morgenshtern_helmet_blacklist"
			)
	);
	public static final TagKey<Item> LEATHER_HELMETS = TagKey.create(
			Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(
					TimothatysTrinketsMod.MODID,
					"morgenshtern_leather_helmets"
			)
	);
	public static final TagKey<Item> CHAINMAIL_HELMETS = TagKey.create(
			Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(
					TimothatysTrinketsMod.MODID,
					"morgenshtern_chainmail_helmets"
			)
	);

	private MorgenshternTags() {
	}
}
