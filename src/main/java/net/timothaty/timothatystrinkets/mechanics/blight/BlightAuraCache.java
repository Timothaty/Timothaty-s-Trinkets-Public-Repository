package net.timothaty.timothatystrinkets.mechanics.blight;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

public final class BlightAuraCache {
	private static final Map<LivingEntity, Sample> CACHE = new WeakHashMap<>();

	private BlightAuraCache() {
	}

	public static Sample sample(LivingEntity living) {
		long now = living.level().getGameTime();
		Sample cached;
		synchronized (CACHE) {
			cached = CACHE.get(living);
		}
		if (cached != null && now >= cached.checkedAt && now - cached.checkedAt < BlightConfig.AURA_CACHE_TICKS) {
			return cached;
		}

		boolean inBlightAura = BlightZoneHelper.isInsideBlightAura(living);
		boolean standingOnBlight = inBlightAura && BlightZoneHelper.isStandingOnBlight(living);
		Sample sample = new Sample(now, inBlightAura, standingOnBlight);
		synchronized (CACHE) {
			CACHE.put(living, sample);
		}
		return sample;
	}

	public static boolean isInsideBlightAura(LivingEntity living) {
		return sample(living).inBlightAura();
	}

	public record Sample(long checkedAt, boolean inBlightAura, boolean standingOnBlight) {
	}
}
