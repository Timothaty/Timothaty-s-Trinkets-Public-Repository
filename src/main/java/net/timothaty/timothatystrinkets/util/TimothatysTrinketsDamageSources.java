package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class TimothatysTrinketsDamageSources {
	private TimothatysTrinketsDamageSources() {
	}

	public static final ResourceKey<DamageType> SOUL_DAMAGE = ResourceKey.create(
			Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "soul_damage")
	);
	public static final ResourceKey<DamageType> DEBTLORD_LASER = ResourceKey.create(
			Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "debtlord_laser")
	);
	public static final ResourceKey<DamageType> MORNING_STARS_JUDGMENT = ResourceKey.create(
			Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "morning_stars_judgment")
	);

	public static DamageSource soulDamage(Level level) {
		return new DamageSource(
				level.registryAccess()
						.registryOrThrow(Registries.DAMAGE_TYPE)
						.getHolderOrThrow(SOUL_DAMAGE)
		);
	}

	public static DamageSource soulDamage(Level level, Entity attacker) {
		return new DamageSource(
				level.registryAccess()
						.registryOrThrow(Registries.DAMAGE_TYPE)
						.getHolderOrThrow(SOUL_DAMAGE),
				attacker,
				attacker
		);
	}

	public static DamageSource debtlordLaser(Level level, net.minecraft.world.entity.Entity debtlord) {
		return new DamageSource(
				level.registryAccess()
						.registryOrThrow(Registries.DAMAGE_TYPE)
						.getHolderOrThrow(DEBTLORD_LASER),
				debtlord,
				debtlord
		);
	}

	public static DamageSource morningStarsJudgment(
			Level level,
			Entity attacker
	) {
		return new DamageSource(
				level.registryAccess()
						.registryOrThrow(Registries.DAMAGE_TYPE)
						.getHolderOrThrow(MORNING_STARS_JUDGMENT),
				attacker,
				attacker
		);
	}
}
