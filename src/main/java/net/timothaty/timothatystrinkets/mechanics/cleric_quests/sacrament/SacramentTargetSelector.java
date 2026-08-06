package net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class SacramentTargetSelector {
	private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));
	private static final TagKey<EntityType<?>> BLACKLIST = TagKey.create(
		Registries.ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "sacrament_target_blacklist")
	);
	private static List<ResourceLocation> eligibleTargets = List.of();

	private SacramentTargetSelector() {
	}

	@SubscribeEvent
	public static void onTagsUpdated(TagsUpdatedEvent event) {
		if (event.shouldUpdateStaticData())
			rebuild(event.getRegistryAccess());
	}

	public static List<ResourceLocation> selectThree(ServerLevel level) {
		if (eligibleTargets.isEmpty())
			rebuild(level.registryAccess());
		if (eligibleTargets.size() < 3) {
			TimothatysTrinketsMod.LOGGER.error("Sacrament hunt cannot start: only {} eligible undead entity types remain after tag filtering; at least 3 are required.", eligibleTargets.size());
			return List.of();
		}
		List<ResourceLocation> shuffled = new ArrayList<>(eligibleTargets);
		shuffle(shuffled, level.getRandom());
		return List.copyOf(shuffled.subList(0, 3));
	}

	private static void rebuild(RegistryAccess registryAccess) {
		Registry<EntityType<?>> registry = registryAccess.registryOrThrow(Registries.ENTITY_TYPE);
		eligibleTargets = registry.getTag(EntityTypeTags.UNDEAD)
			.map(named -> named.stream()
				.filter(holder -> !holder.is(BOSSES) && !holder.is(BLACKLIST))
				.map(Holder::unwrapKey)
				.flatMap(java.util.Optional::stream)
				.map(key -> key.location())
				.distinct()
				.toList())
			.orElse(List.of());
	}

	private static void shuffle(List<ResourceLocation> values, RandomSource random) {
		for (int index = values.size() - 1; index > 0; index--) {
			int other = random.nextInt(index + 1);
			ResourceLocation value = values.get(index);
			values.set(index, values.get(other));
			values.set(other, value);
		}
	}
}
