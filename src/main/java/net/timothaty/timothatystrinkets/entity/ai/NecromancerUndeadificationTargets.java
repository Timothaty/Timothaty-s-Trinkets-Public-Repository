package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class NecromancerUndeadificationTargets {
	private static final TagKey<EntityType<?>> UNDEADIFY_RESTRICTED = TagKey.create(
		Registries.ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undeadify_restricted")
	);

	private NecromancerUndeadificationTargets() {
	}

	public static boolean shouldUseMagicDamageCast(LivingEntity target) {
		return target instanceof Player || target.getType().is(UNDEADIFY_RESTRICTED);
	}

	public static boolean canReceiveUndeadification(LivingEntity target) {
		return !shouldUseMagicDamageCast(target);
	}
}
