package net.timothaty.timothatystrinkets.api.damage;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Public helpers for creating and identifying holy damage sources.
 *
 * <pre>{@code
 * target.hurt(HolyDamageApi.source(level, attacker), 6.0F);
 * if (HolyDamageApi.isHoly(source)) {
 *     // Apply an interaction specific to holy damage.
 * }
 * }</pre>
 *
 * <p>Addons can also add their own damage types to the
 * {@code timothatys_trinkets:holy_damage} damage type tag.</p>
 */
public final class HolyDamageApi {
	public static final ResourceKey<DamageType> HOLY_DAMAGE_TYPE = ResourceKey.create(
			Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "holy_damage")
	);
	public static final TagKey<DamageType> HOLY_DAMAGE_TAG = TagKey.create(
			Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "holy_damage")
	);

	private HolyDamageApi() {
	}

	public static boolean isHoly(@Nullable DamageSource source) {
		return source != null && source.is(HOLY_DAMAGE_TAG);
	}

	public static DamageSource source(Level level, @Nullable Entity attacker) {
		Holder<DamageType> damageType = getUniversalDamageType(level);
		return attacker == null
				? new DamageSource(damageType)
				: new DamageSource(damageType, attacker, attacker);
	}

	public static DamageSource indirectSource(Level level, Entity directEntity, @Nullable Entity attacker) {
		if (directEntity == null)
			throw new IllegalArgumentException("Holy damage direct entity cannot be null");
		return new DamageSource(getUniversalDamageType(level), directEntity, attacker);
	}

	private static Holder<DamageType> getUniversalDamageType(Level level) {
		if (level == null)
			throw new IllegalArgumentException("Level cannot be null when creating holy damage");
		return level.registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(HOLY_DAMAGE_TYPE);
	}
}
