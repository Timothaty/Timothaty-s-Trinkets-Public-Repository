package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.fire.CustomSweepVisualState;
import net.timothaty.timothatystrinkets.mechanics.pact.PactOfAllianceHelper;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsDamageSources;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class HubrisStrikeResolver {
	private static final Map<UUID, AttackContext> CONTEXTS = new HashMap<>();
	private static long nextAttackSerial;

	private HubrisStrikeResolver() {
	}

	public static void beginAttack(ServerPlayer attacker, Entity primaryTarget) {
		if (attacker == null)
			return;

		CONTEXTS.remove(attacker.getUUID());
		HubrisState.Strike strike = HubrisState.currentStrike(attacker);
		if (strike == null
				|| !attacker.hasEffect(TimothatysTrinketsModMobEffects.HUBRIS)
				|| !attacker.isAlive()
				|| attacker.isDeadOrDying()
				|| attacker.isRemoved()
				|| attacker.isSpectator())
			return;

		float attackStrength = attacker.getAttackStrengthScale(0.5F);
		if (attackStrength < HubrisData.MIN_ATTACK_STRENGTH)
			return;

		ItemStack weapon = attacker.getMainHandItem();
		boolean sword = weapon.is(ItemTags.SWORDS);
		if (weapon.isEmpty() || !sword && !weapon.is(HubrisData.HEAVY_ARMS))
			return;
		if (!(primaryTarget instanceof LivingEntity livingTarget) || !isValidTarget(attacker, livingTarget))
			return;
		if (!(livingTarget.getMaxHealth() > attacker.getMaxHealth() * HubrisData.TARGET_HEALTH_THRESHOLD))
			return;

		CONTEXTS.put(attacker.getUUID(), new AttackContext(
				++nextAttackSerial,
				livingTarget.getId(),
				strike.index(),
				strike.multiplier(),
				attackStrength,
				sword,
				getSweepingEdgeLevel(weapon, attacker)
		));
	}

	public static void markActualSweep(ServerPlayer attacker, Entity primaryTarget, boolean sweeping) {
		AttackContext context = current(attacker);
		if (!sweeping || context == null || !context.sword || primaryTarget == null
				|| context.primaryTargetId != primaryTarget.getId())
			return;
		context.actualSweep = true;
		CustomSweepVisualState.markPrideful(attacker);
	}

	public static void amplifyIncomingDamage(LivingIncomingDamageEvent event) {
		if (event == null || event.getAmount() <= 0.0F || isSoulDamage(event.getSource()))
			return;
		if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)
				|| event.getSource().getDirectEntity() != attacker
				|| !event.getSource().is(DamageTypes.PLAYER_ATTACK))
			return;

		AttackContext context = current(attacker);
		LivingEntity target = event.getEntity();
		if (context == null || !isTargetPartOfStrike(attacker, target, context)
				|| context.hits.containsKey(target.getId()))
			return;

		float amplifiedDamage = event.getAmount() * context.strikeMultiplier;
		event.setAmount(amplifiedDamage);
		int fullSoulDamage = Mth.floor(amplifiedDamage * HubrisData.SOUL_DAMAGE_RATIO);
		context.hits.put(target.getId(), new PendingHit(
				target,
				target.getId() == context.primaryTargetId,
				fullSoulDamage
		));
	}

	public static void markDamageApplied(LivingDamageEvent.Post event) {
		if (event == null || isSoulDamage(event.getSource()))
			return;
		if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)
				|| event.getSource().getDirectEntity() != attacker
				|| !event.getSource().is(DamageTypes.PLAYER_ATTACK))
			return;

		AttackContext context = current(attacker);
		PendingHit hit = context == null ? null : context.hits.get(event.getEntity().getId());
		if (hit != null) {
			hit.damageApplied = true;
			confirmPrimaryStrike(attacker, context, hit);
		}
	}

	public static void markDefended(LivingEntity target, DamageSource source, DefenseKind kind) {
		if (target == null || source == null || kind == null || isSoulDamage(source))
			return;
		if (!(source.getEntity() instanceof ServerPlayer attacker)
				|| source.getDirectEntity() != attacker
				|| !source.is(DamageTypes.PLAYER_ATTACK))
			return;

		AttackContext context = current(attacker);
		PendingHit hit = context == null ? null : context.hits.get(target.getId());
		if (hit != null) {
			hit.defenseKind = kind;
			confirmPrimaryStrike(attacker, context, hit);
		}
	}

	public static void finishAttack(ServerPlayer attacker) {
		AttackContext context = current(attacker);
		if (context == null)
			return;

		boolean thornConsumed = context.thornConsumed;
		try {
			PendingHit primary = context.hits.get(context.primaryTargetId);
			if (!isResolved(primary) || !context.thornConsumed
					|| !attacker.isAlive() || attacker.isDeadOrDying() || attacker.isRemoved())
				return;
			for (PendingHit hit : context.hits.values()) {
				if (!isResolved(hit))
					continue;
				applySoulDamage(attacker, hit);
			}
		} finally {
			CONTEXTS.remove(attacker.getUUID(), context);
			if (thornConsumed)
				HubrisState.finishPendingEnd(attacker);
		}
	}

	public static void clear(Player player) {
		if (player != null)
			CONTEXTS.remove(player.getUUID());
	}

	public static void clearAll() {
		CONTEXTS.clear();
	}

	private static AttackContext current(Player player) {
		return player == null ? null : CONTEXTS.get(player.getUUID());
	}

	private static boolean isTargetPartOfStrike(ServerPlayer attacker, LivingEntity target, AttackContext context) {
		if (target.getId() == context.primaryTargetId)
			return isValidTarget(attacker, target);
		return context.actualSweep
				&& context.sweepingEdgeLevel > 0
				&& target.getId() != context.primaryTargetId
				&& isValidTarget(attacker, target);
	}

	private static boolean isValidTarget(Player attacker, LivingEntity target) {
		if (target == attacker
				|| !target.isAlive()
				|| target.isDeadOrDying()
				|| target.isRemoved()
				|| target instanceof ArmorStand)
			return false;
		if (target instanceof Player player && player.isSpectator())
			return false;
		return !PactOfAllianceHelper.areAllied(attacker, target);
	}

	private static boolean isResolved(PendingHit hit) {
		return hit != null && (hit.damageApplied || hit.defenseKind != null);
	}

	private static void confirmPrimaryStrike(ServerPlayer attacker, AttackContext context, PendingHit hit) {
		if (!hit.primary || context.thornConsumed || !isResolved(hit))
			return;
		context.thornConsumed = HubrisState.consumeThorn(attacker);
	}

	private static void applySoulDamage(ServerPlayer attacker, PendingHit hit) {
		LivingEntity target = hit.target;
		if (hit.fullSoulDamage <= 0 || !target.isAlive() || target.isDeadOrDying() || target.isRemoved()
				|| !isValidTarget(attacker, target))
			return;

		float amount = hit.defenseKind == null
				? hit.fullSoulDamage
				: hit.fullSoulDamage * HubrisData.BLOCKED_SOUL_DAMAGE_RATIO;
		if (amount <= 0.0F)
			return;

		Vec3 movement = target.getDeltaMovement();
		boolean hurtMarked = target.hurtMarked;
		boolean hadImpulse = target.hasImpulse;
		target.hurt(TimothatysTrinketsDamageSources.soulDamage(target.level(), attacker), amount);
		if (!target.isRemoved()) {
			target.setDeltaMovement(movement);
			target.hurtMarked = hurtMarked;
			target.hasImpulse = hadImpulse;
		}
	}

	private static int getSweepingEdgeLevel(ItemStack weapon, Player attacker) {
		try {
			Holder<Enchantment> enchantment = attacker.level()
					.registryAccess()
					.registryOrThrow(Registries.ENCHANTMENT)
					.getHolderOrThrow(Enchantments.SWEEPING_EDGE);
			return weapon.getEnchantmentLevel(enchantment);
		} catch (RuntimeException ignored) {
			return 0;
		}
	}

	private static boolean isSoulDamage(DamageSource source) {
		return source != null && source.is(TimothatysTrinketsDamageSources.SOUL_DAMAGE);
	}

	public enum DefenseKind {
		VANILLA_SHIELD,
		DUELIST_CENTER_PARRY,
		DUELIST_SIDE_DEFLECT,
		UNDEAD_KNIGHT_BLOCK,
		BEATIFIC_PALLIUM
	}

	private static final class AttackContext {
		private final long attackSerial;
		private final int primaryTargetId;
		private final int strikeIndex;
		private final float strikeMultiplier;
		private final float capturedAttackStrength;
		private final boolean sword;
		private final int sweepingEdgeLevel;
		private final Map<Integer, PendingHit> hits = new LinkedHashMap<>();
		private boolean actualSweep;
		private boolean thornConsumed;

		private AttackContext(
				long attackSerial,
				int primaryTargetId,
				int strikeIndex,
				float strikeMultiplier,
				float capturedAttackStrength,
				boolean sword,
				int sweepingEdgeLevel
		) {
			this.attackSerial = attackSerial;
			this.primaryTargetId = primaryTargetId;
			this.strikeIndex = strikeIndex;
			this.strikeMultiplier = strikeMultiplier;
			this.capturedAttackStrength = capturedAttackStrength;
			this.sword = sword;
			this.sweepingEdgeLevel = sweepingEdgeLevel;
		}
	}

	private static final class PendingHit {
		private final LivingEntity target;
		private final boolean primary;
		private final int fullSoulDamage;
		private boolean damageApplied;
		private DefenseKind defenseKind;

		private PendingHit(LivingEntity target, boolean primary, int fullSoulDamage) {
			this.target = target;
			this.primary = primary;
			this.fullSoulDamage = fullSoulDamage;
		}
	}
}
