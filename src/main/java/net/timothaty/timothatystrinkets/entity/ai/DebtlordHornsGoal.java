package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordDamageScaling;

import net.neoforged.neoforge.common.ItemAbilities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class DebtlordHornsGoal extends Goal {
	public static final int CAST_DURATION_TICKS = 20;
	public static final int IMPACT_TICK = 11;
	public static final int COOLDOWN_TICKS = 2 * 20;
	public static final double TRIGGER_RANGE = 3.5D;
	public static final float BASE_DAMAGE = 10.0F;
	public static final float MISSING_HEALTH_DAMAGE_RATIO = 0.2F;
	private static final float LIFE_STEAL_RATIO = 0.2F;
	private static final double TRIGGER_RANGE_SQR = TRIGGER_RANGE * TRIGGER_RANGE;
	private static final double MAX_VERTICAL_DISTANCE = 2.75D;
	private static final double MIN_FORWARD_DOT = 0.35D;
	private static final double KNOCKBACK_STRENGTH = 1.45D;
	private static final double KNOCKBACK_UPWARD = 0.35D;
	private static final float BLOCKED_SHIELD_DURABILITY_RATIO = 0.25F;

	private final DebtlordEntity debtlord;
	private long nextAvailableGameTime;
	private long nextDisarmAvailableGameTime;

	public DebtlordHornsGoal(DebtlordEntity debtlord) {
		this.debtlord = debtlord;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return debtlord.isAlive()
			&& debtlord.onGround()
			&& !debtlord.isUsingAbility()
			&& debtlord.wantsCombatIntent(DebtlordEntity.CombatIntent.HORNS)
			&& debtlord.level().getGameTime() >= nextAvailableGameTime
			&& isTargetInRange(debtlord.getTarget());
	}

	@Override
	public boolean canContinueToUse() {
		return debtlord.isAlive() && (debtlord.isUsingHorns() || debtlord.isUsingDisarm());
	}

	@Override
	public void start() {
		LivingEntity target = debtlord.getTarget();
		if (target == null)
			return;

		long gameTime = debtlord.level().getGameTime();
		nextAvailableGameTime = gameTime + COOLDOWN_TICKS;
		if (gameTime >= nextDisarmAvailableGameTime
			&& DebtlordDisarmAbility.canDisarmTarget(target)
			&& debtlord.getRandom().nextFloat() < DebtlordDisarmAbility.SELECTION_CHANCE) {
			nextDisarmAvailableGameTime = gameTime + DebtlordDisarmAbility.COOLDOWN_TICKS;
			debtlord.startDisarmCast(target, DebtlordDisarmAbility.CAST_DURATION_TICKS);
			return;
		}

		debtlord.startHornsCast(target, CAST_DURATION_TICKS);
		debtlord.level().playSound(null, debtlord.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.0F, 1.0F);
	}

	@Override
	public void tick() {
		debtlord.lockAbilityPosition();
	}

	@Override
	public void stop() {
		if (debtlord.isUsingDisarm()) {
			debtlord.finishDisarmCast();
		} else {
			debtlord.finishHornsCast();
		}
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private boolean isTargetInRange(LivingEntity target) {
		if (target == null || !target.isAlive())
			return false;

		double dx = target.getX() - debtlord.getX();
		double dz = target.getZ() - debtlord.getZ();
		return dx * dx + dz * dz <= TRIGGER_RANGE_SQR
			&& Math.abs(target.getY() - debtlord.getY()) <= MAX_VERTICAL_DISTANCE;
	}

	public static void performImpact(DebtlordEntity debtlord, LivingEntity target) {
		Vec3 horizontalDirection = getValidImpactDirection(debtlord, target);
		if (horizontalDirection == null)
			return;

		float missingHealth = Math.max(0.0F, target.getMaxHealth() - target.getHealth());
		float baseDamage = BASE_DAMAGE + missingHealth * MISSING_HEALTH_DAMAGE_RATIO;
		float appliedMultiplier = DebtlordDamageScaling.getAppliedMultiplier(target, DebtlordDamageScaling.DIRECT_ATTACK_IRON_GOLEM_MULTIPLIER);
		float damage = baseDamage * appliedMultiplier;
		ItemStack blockingShield = getBlockingShield(target);
		int shieldDamageBeforeHit = blockingShield.isEmpty() ? 0 : blockingShield.getDamageValue();
		float effectiveHealthBefore = DebtlordDamageScaling.getEffectiveHealth(target);
		boolean damaged = target.hurt(debtlord.damageSources().mobAttack(debtlord), damage);
		if (!damaged) {
			damageBlockingShield(target, blockingShield, shieldDamageBeforeHit);
			return;
		}
		float actualDamage = DebtlordDamageScaling.getActualDamage(target, effectiveHealthBefore);
		float lifeStealDamage = DebtlordDamageScaling.normalizeDamageForLifeSteal(target, actualDamage, appliedMultiplier);
		if (lifeStealDamage > 0.0F)
			debtlord.heal(lifeStealDamage * LIFE_STEAL_RATIO);

		Vec3 currentMotion = target.getDeltaMovement();
		target.setDeltaMovement(
			currentMotion.x * 0.2D + horizontalDirection.x * KNOCKBACK_STRENGTH,
			Math.max(currentMotion.y, KNOCKBACK_UPWARD),
			currentMotion.z * 0.2D + horizontalDirection.z * KNOCKBACK_STRENGTH
		);
		target.hurtMarked = true;

		if (debtlord.level() instanceof ServerLevel serverLevel) {
			Vec3 impactPosition = target.position().add(0.0D, target.getBbHeight() * 0.65D, 0.0D);
			spawnBloodSpray(serverLevel, target, impactPosition, horizontalDirection);
			serverLevel.playSound(null, target.blockPosition(), TimothatysTrinketsModSounds.HORN_HIT.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
		}
	}

	static Vec3 getValidImpactDirection(DebtlordEntity debtlord, LivingEntity target) {
		if (target == null || !target.isAlive() || debtlord.isAlliedTo(target))
			return null;

		Vec3 toTarget = target.position().subtract(debtlord.position());
		Vec3 horizontalDirection = new Vec3(toTarget.x, 0.0D, toTarget.z);
		double horizontalDistanceSqr = horizontalDirection.lengthSqr();
		if (horizontalDistanceSqr > TRIGGER_RANGE_SQR || horizontalDistanceSqr < 1.0E-6D
			|| Math.abs(target.getY() - debtlord.getY()) > MAX_VERTICAL_DISTANCE)
			return null;

		horizontalDirection = horizontalDirection.normalize();
		Vec3 lookDirection = debtlord.getLookAngle();
		Vec3 horizontalLook = new Vec3(lookDirection.x, 0.0D, lookDirection.z).normalize();
		return horizontalLook.dot(horizontalDirection) >= MIN_FORWARD_DOT ? horizontalDirection : null;
	}

	private static void spawnBloodSpray(ServerLevel serverLevel, LivingEntity target, Vec3 origin, Vec3 direction) {
		Vec3 sideways = new Vec3(-direction.z, 0.0D, direction.x);
		for (int i = 0; i < 32; i++) {
			double forwardSpeed = 0.65D + serverLevel.getRandom().nextDouble() * 0.55D;
			double lateralSpeed = (serverLevel.getRandom().nextDouble() - 0.5D) * 0.7D;
			double upwardSpeed = 0.25D + serverLevel.getRandom().nextDouble() * 0.45D;
			double spawnX = origin.x + (serverLevel.getRandom().nextDouble() - 0.5D) * target.getBbWidth() * 0.65D;
			double spawnY = origin.y + (serverLevel.getRandom().nextDouble() - 0.5D) * target.getBbHeight() * 0.35D;
			double spawnZ = origin.z + (serverLevel.getRandom().nextDouble() - 0.5D) * target.getBbWidth() * 0.65D;
			serverLevel.sendParticles(
				TimothatysTrinketsModParticleTypes.BLOOD_BIT.get(),
				spawnX, spawnY, spawnZ,
				0,
				direction.x * forwardSpeed + sideways.x * lateralSpeed,
				upwardSpeed,
				direction.z * forwardSpeed + sideways.z * lateralSpeed,
				1.0D
			);
		}
	}

	private static ItemStack getBlockingShield(LivingEntity target) {
		if (!(target instanceof Player player) || !player.isBlocking() || player.isCreative() || player.isSpectator())
			return ItemStack.EMPTY;

		ItemStack shield = player.getUseItem();
		if (shield.isEmpty() || !shield.canPerformAction(ItemAbilities.SHIELD_BLOCK) || !shield.isDamageableItem())
			return ItemStack.EMPTY;
		return shield;
	}

	private static void damageBlockingShield(LivingEntity target, ItemStack shield, int damageBeforeHit) {
		if (!(target instanceof Player player) || shield.isEmpty())
			return;

		int totalDurabilityCost = Math.max(1, Mth.ceil(shield.getMaxDamage() * BLOCKED_SHIELD_DURABILITY_RATIO));
		int resultingDamage = Math.min(shield.getMaxDamage(), damageBeforeHit + totalDurabilityCost);
		if (resultingDamage >= shield.getMaxDamage()) {
			player.onEquippedItemBroken(shield.getItem(), LivingEntity.getSlotForHand(player.getUsedItemHand()));
			shield.shrink(1);
		} else {
			shield.setDamageValue(resultingDamage);
		}
	}
}
