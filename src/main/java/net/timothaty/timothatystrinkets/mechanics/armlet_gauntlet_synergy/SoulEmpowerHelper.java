package net.timothaty.timothatystrinkets.mechanics.armlet_gauntlet_synergy;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsAttributeHelper;

public final class SoulEmpowerHelper {
	private SoulEmpowerHelper() {
	}

	public static int getLevel(LivingEntity entity) {
		if (entity == null) {
			return 0;
		}
		MobEffectInstance effect = entity.getEffect(TimothatysTrinketsModMobEffects.SOUL_EMPOWER);
		return effect == null ? 0 : Math.min(ArmletGauntletSynergyData.MAX_SOUL_EMPOWER_LEVEL, effect.getAmplifier() + 1);
	}

	public static boolean addLevel(LivingEntity entity) {
		int currentLevel = getLevel(entity);
		if (entity == null || currentLevel >= ArmletGauntletSynergyData.MAX_SOUL_EMPOWER_LEVEL) {
			return false;
		}

		int nextAmplifier = currentLevel;
		entity.addEffect(new MobEffectInstance(
				TimothatysTrinketsModMobEffects.SOUL_EMPOWER,
				ArmletGauntletSynergyData.SOUL_EMPOWER_DURATION_TICKS,
				nextAmplifier,
				false,
				false,
				true
		));
		refreshAttributeModifiers(entity);
		return true;
	}

	public static void refreshAttributeModifiers(LivingEntity entity) {
		setAttributeModifiersForLevel(entity, getLevel(entity));
	}

	public static void setAttributeModifiersForLevel(LivingEntity entity, int requestedLevel) {
		int level = Math.max(0, Math.min(ArmletGauntletSynergyData.MAX_SOUL_EMPOWER_LEVEL, requestedLevel));
		if (entity instanceof Player player && HolyRosariumHelper.suppressesUnholyRelics(player))
			level = 0;
		TimothatysTrinketsAttributeHelper.setModifier(
				entity,
				Attributes.ARMOR,
				ArmletGauntletSynergyData.ARMOR_MODIFIER_ID,
				level * ArmletGauntletSynergyData.ARMOR_PER_LEVEL,
				AttributeModifier.Operation.ADD_VALUE,
				level > 0
		);
		TimothatysTrinketsAttributeHelper.setModifier(
				entity,
				Attributes.MOVEMENT_SPEED,
				ArmletGauntletSynergyData.MOVEMENT_SPEED_MODIFIER_ID,
				level * ArmletGauntletSynergyData.MOVEMENT_SPEED_PER_LEVEL,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
				level > 0
		);
	}

	public static void clearAttributeModifiers(LivingEntity entity) {
		TimothatysTrinketsAttributeHelper.removeModifier(entity, Attributes.ARMOR, ArmletGauntletSynergyData.ARMOR_MODIFIER_ID);
		TimothatysTrinketsAttributeHelper.removeModifier(entity, Attributes.MOVEMENT_SPEED, ArmletGauntletSynergyData.MOVEMENT_SPEED_MODIFIER_ID);
	}
}
