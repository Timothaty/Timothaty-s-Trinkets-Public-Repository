package net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet;

import java.util.Map;
import java.util.WeakHashMap;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisStrikeResolver;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class DuelistGuardSideDeflect {
	private static final Map<DamageContainer, PendingStun> PENDING_STUNS = new WeakHashMap<>();

	private DuelistGuardSideDeflect() {
	}

	public static boolean tryHandle(LivingIncomingDamageEvent event, Player defender, LivingEntity attacker, DuelistGuardDirection guardDirection) {
		if (event == null || defender == null || attacker == null)
			return false;
		if (!guardDirection.isSide() || DuelistGuardState.isGuardWeaponOnCooldown(defender))
			return false;

		float incomingDamage = event.getAmount();
		HubrisStrikeResolver.markDefended(
				defender,
				event.getSource(),
				HubrisStrikeResolver.DefenseKind.DUELIST_SIDE_DEFLECT
		);
		float blockRatio = DuelistGauntletDurability.isBoss(attacker) ? DuelistGuardData.SIDE_DEFLECT_BOSS_BLOCK_RATIO : DuelistGuardData.SIDE_DEFLECT_BLOCK_RATIO;
		float blockedDamage = incomingDamage * blockRatio;
		float passedDamage = Math.max(0.0F, incomingDamage - blockedDamage);
		event.setAmount(passedDamage);

		boolean hasStamina = DuelistGuardState.consumeSideDeflectStamina(defender);
		DuelistGauntletDurability.damageForSideDeflect(defender, attacker);
		DuelistGuardState.applySideDeflectWeaponCooldown(defender);
		DuelistGuardKnockback.knockAttackerAway(defender, attacker, guardDirection);
		int stunTicks = attacker instanceof Player ? DuelistGuardData.SIDE_DEFLECT_PLAYER_STUN_TICKS : DuelistGuardData.SIDE_DEFLECT_STUN_TICKS;
		PENDING_STUNS.put(event.getContainer(), new PendingStun(attacker, defender, stunTicks));
		defender.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.RIPOSTE, DuelistGuardData.SIDE_DEFLECT_RIPOSTE_TICKS, 0, false, true, true));
		DuelistGuardParticles.spawnSideDeflectSparks(defender, attacker, guardDirection);
		playSideDeflectSound(defender);

		DuelistGuardDebug.show(defender, "Deflected " + guardDirection + " | Stamina " + Math.round(DuelistGuardState.getStamina(defender)), ChatFormatting.AQUA);
		if (!hasStamina) {
			DuelistGuardState.triggerGuardBreak(defender);
			DuelistGuardDebug.show(defender, "Guard BROKEN | Deflected " + guardDirection, ChatFormatting.RED);
		}
		return true;
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void applyPendingStun(LivingDamageEvent.Pre event) {
		PendingStun pending = PENDING_STUNS.remove(event.getContainer());
		if (pending == null)
			return;

		TimothatysTrinketsStunHelper.tryApplyStun(pending.attacker(), pending.defender(), pending.ticks());
	}

	private static void playSideDeflectSound(Player defender) {
		defender.level().playSound(null, defender.blockPosition(), TimothatysTrinketsModSounds.SWORD_PARRY.get(), SoundSource.PLAYERS, 0.7F, 1.18F + defender.getRandom().nextFloat() * 0.14F);
	}

	private record PendingStun(LivingEntity attacker, Player defender, int ticks) {
	}
}
