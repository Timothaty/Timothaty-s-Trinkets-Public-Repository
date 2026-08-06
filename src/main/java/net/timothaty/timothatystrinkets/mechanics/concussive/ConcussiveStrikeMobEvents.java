package net.timothaty.timothatystrinkets.mechanics.concussive;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.util.ConcussiveStrikeData;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class ConcussiveStrikeMobEvents {
	private ConcussiveStrikeMobEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event == null || event.getNewDamage() <= 0.0F)
			return;

		LivingEntity target = event.getEntity();
		if (target == null || !target.isAlive())
			return;

		DamageSource source = event.getSource();
		if (!isMobMeleeAttack(source))
			return;

		Entity attackerEntity = source.getEntity();
		if (!(attackerEntity instanceof Mob attacker))
			return;
		if (attacker.level().isClientSide() || target == attacker)
			return;
		if (target.hasEffect(TimothatysTrinketsModMobEffects.STUN_IMMUNITY))
			return;

		ItemStack weapon = attacker.getMainHandItem();
		if (weapon.isEmpty() || !weapon.is(ConcussiveStrikeData.COMPAT_ITEMS))
			return;

		int concussiveLevel = getConcussiveStrikeLevel(weapon, attacker.level());
		if (concussiveLevel <= 0)
			return;
		if (attacker.getRandom().nextFloat() >= ConcussiveStrikeData.MOB_LISTED_CHANCE)
			return;

		boolean applied = TimothatysTrinketsStunHelper.tryApplyStunSilently(target, attacker, ConcussiveStrikeData.getStunTicks(concussiveLevel));
		if (applied) {
			playConcussiveStrikeSound(attacker, target);
		}
	}

	private static int getConcussiveStrikeLevel(ItemStack weapon, Level level) {
		try {
			Holder<Enchantment> holder = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(ConcussiveStrikeData.ENCHANTMENT);
			return weapon.getEnchantmentLevel(holder);
		} catch (Exception ignored) {
			return 0;
		}
	}

	private static void playConcussiveStrikeSound(Mob attacker, LivingEntity target) {
		Level level = attacker.level();
		if (level.isClientSide())
			return;

		level.playSound(
				null,
				target.blockPosition(),
				TimothatysTrinketsModSounds.CONCUSSIVE_STRIKE.get(),
				SoundSource.HOSTILE,
				0.6F,
				0.85F + attacker.getRandom().nextFloat() * 0.1F
		);
	}

	private static boolean isMobMeleeAttack(DamageSource source) {
		if (source == null)
			return false;

		Entity attacker = source.getEntity();
		Entity direct = source.getDirectEntity();
		return attacker instanceof Mob && direct == attacker;
	}
}
