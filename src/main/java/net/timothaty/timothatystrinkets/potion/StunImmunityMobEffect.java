package net.timothaty.timothatystrinkets.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class StunImmunityMobEffect extends MobEffect {
	public StunImmunityMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -20480);
	}
}