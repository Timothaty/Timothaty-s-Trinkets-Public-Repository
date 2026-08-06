package net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

public final class DuelistMeleeDamage {
	private DuelistMeleeDamage() {
	}

	public static boolean isDirectMeleeDamage(DamageSource source) {
		if (source == null)
			return false;
		if (!source.is(DamageTypes.PLAYER_ATTACK) && !source.is(DamageTypes.MOB_ATTACK) && !source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))
			return false;

		Entity attacker = source.getEntity();
		return attacker instanceof LivingEntity && source.getDirectEntity() == attacker;
	}

	public static LivingEntity getLivingAttacker(DamageSource source) {
		return isDirectMeleeDamage(source) && source.getEntity() instanceof LivingEntity attacker ? attacker : null;
	}

	public static boolean hasMainHandSword(LivingEntity entity) {
		if (entity == null)
			return false;
		ItemStack stack = entity.getMainHandItem();
		return stack != null && !stack.isEmpty() && stack.getItem() instanceof SwordItem;
	}
}
