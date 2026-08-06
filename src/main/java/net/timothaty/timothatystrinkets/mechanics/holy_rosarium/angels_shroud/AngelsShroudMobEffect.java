package net.timothaty.timothatystrinkets.mechanics.holy_rosarium.angels_shroud;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class AngelsShroudMobEffect extends MobEffect {
	public AngelsShroudMobEffect() {
		super(MobEffectCategory.BENEFICIAL, AngelsShroudData.GOLD_RGB);
	}
}
