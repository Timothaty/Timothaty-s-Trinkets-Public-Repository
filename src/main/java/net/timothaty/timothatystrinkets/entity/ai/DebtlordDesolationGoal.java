package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public final class DebtlordDesolationGoal extends Goal {
	public static final int CAST_DURATION_TICKS = 70;
	public static final int EFFECT_TICK = 30;
	public static final int EFFECT_DURATION_TICKS = 16 * 20;

	private final DebtlordEntity debtlord;
	private Player target;

	public DebtlordDesolationGoal(DebtlordEntity debtlord) {
		this.debtlord = debtlord;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (!debtlord.canStartDesolationAbility() || !(debtlord.level() instanceof ServerLevel serverLevel))
			return false;

		if (debtlord.getTarget() instanceof Player currentPlayer && isValidPlayer(currentPlayer)) {
			target = currentPlayer;
			return true;
		}

		target = serverLevel.getNearestPlayer(debtlord, debtlord.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE));
		return isValidPlayer(target);
	}

	@Override
	public boolean canContinueToUse() {
		return debtlord.isAlive() && debtlord.isUsingDesolation();
	}

	@Override
	public void start() {
		debtlord.startDesolationCast(target, CAST_DURATION_TICKS);
	}

	@Override
	public void tick() {
		debtlord.lockAbilityPosition();
		int remainingTicks = debtlord.getDesolationCastTicks();
		int elapsedTicks = CAST_DURATION_TICKS - remainingTicks + 1;
		if (elapsedTicks == EFFECT_TICK && isValidPlayer(target))
			target.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.DESOLATED, EFFECT_DURATION_TICKS, 0, false, true, true), debtlord);

		if (remainingTicks > 1) {
			debtlord.setDesolationCastTicks(remainingTicks - 1);
		} else {
			debtlord.finishDesolationCast();
			target = null;
		}
	}

	@Override
	public void stop() {
		debtlord.finishDesolationCast();
		target = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private static boolean isValidPlayer(Player player) {
		return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
	}
}
