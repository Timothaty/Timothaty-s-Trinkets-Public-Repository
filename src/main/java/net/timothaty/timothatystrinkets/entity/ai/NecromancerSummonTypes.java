package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public final class NecromancerSummonTypes {
	private static final TagKey<EntityType<?>> NECRO_SUMMONS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "necro_summons"));

	private NecromancerSummonTypes() {
	}

	public static boolean hasAvailableSummonType(ServerLevel serverLevel) {
		return !collectAvailableSummonTypes(serverLevel).isEmpty();
	}

	public static EntityType<?> pickSummonType(ServerLevel serverLevel, RandomSource random) {
		List<EntityType<?>> summonTypes = collectAvailableSummonTypes(serverLevel);
		if (summonTypes.isEmpty()) {
			return null;
		}

		return summonTypes.get(random.nextInt(summonTypes.size()));
	}

	private static List<EntityType<?>> collectAvailableSummonTypes(ServerLevel serverLevel) {
		List<EntityType<?>> availableTypes = new ArrayList<>();
		var entityTypeRegistry = serverLevel.registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
		var summonTag = entityTypeRegistry.getTag(NECRO_SUMMONS);
		if (summonTag.isEmpty()) {
			return availableTypes;
		}

		HolderSet.Named<EntityType<?>> summonTypes = summonTag.get();
		for (int i = 0; i < summonTypes.size(); i++) {
			Holder<EntityType<?>> candidate = summonTypes.get(i);
			availableTypes.add(candidate.value());
		}

		return availableTypes;
	}
}
