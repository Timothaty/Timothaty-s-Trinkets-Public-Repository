package net.timothaty.timothatystrinkets.mechanics.venom;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsEffectSoundHandler;
import net.timothaty.timothatystrinkets.util.TimothatysCuriosHelper;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class VenomSphereCombatEvents {
	private static final float FULLY_CHARGED_ATTACK_THRESHOLD = 0.9F;

	private VenomSphereCombatEvents() {
	}

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		if (event == null)
			return;

		LivingEntity target = event.getEntity();
		if (target == null || !target.isAlive())
			return;

		DamageSource source = event.getSource();
		if (source == null)
			return;

		Entity attackerEntity = source.getEntity();
		if (!(attackerEntity instanceof Player player))
			return;

		Level level = player.level();
		if (level.isClientSide())
			return;

		if (target == player)
			return;
		if (PactOfAllianceHelper.areAllied(player, target))
			return;
		if (!isDirectFullyChargedMeleeAttack(source, player))
			return;
		if (!hasVenomSphereEquipped(player))
			return;

		VenomSphereTargetTracker.StackResult result = VenomSphereTargetTracker.recordChargedHit(player, target, level.getGameTime());
		if (result.rejected())
			return;

		if (result.stackAdded()) {
			TimothatysTrinketsEffectSoundHandler.playCorrosiveToxicityStackSound(target);
		}
	}

	private static boolean isDirectFullyChargedMeleeAttack(DamageSource source, Player player) {
		if (!source.is(DamageTypes.PLAYER_ATTACK))
			return false;
		if (source.getEntity() != player || source.getDirectEntity() != player)
			return false;
		return player.getAttackStrengthScale(0.5F) >= FULLY_CHARGED_ATTACK_THRESHOLD;
	}

	private static boolean hasVenomSphereEquipped(Player player) {
		return TimothatysCuriosHelper.hasCurio(player, VenomSphereData.VENOM_SPHERE_ID);
	}
}
