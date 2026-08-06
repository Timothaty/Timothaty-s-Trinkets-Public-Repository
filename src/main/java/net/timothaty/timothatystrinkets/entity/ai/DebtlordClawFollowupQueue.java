package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.minecraft.world.entity.LivingEntity;

public final class DebtlordClawFollowupQueue {
	public enum Reason {
		STOMP_COMBO,
		STEP_RECOVERY,
		PILLAR_RECOVERY
	}

	private Entry pending;

	public void offer(DebtlordEntity debtlord, LivingEntity target, int delayTicks, int windowTicks, Reason reason) {
		if (!isValid(debtlord, target) || windowTicks <= 0)
			return;

		long notBefore = debtlord.level().getGameTime() + Math.max(0, delayTicks);
		pending = new Entry(target, notBefore, notBefore + windowTicks, reason);
	}

	public Entry peekReady(DebtlordEntity debtlord) {
		if (!validate(debtlord))
			return null;
		long gameTime = debtlord.level().getGameTime();
		return gameTime >= pending.notBeforeGameTime ? pending : null;
	}

	public Entry consumeReady(DebtlordEntity debtlord) {
		Entry ready = peekReady(debtlord);
		if (ready != null)
			pending = null;
		return ready;
	}

	public void clear() {
		pending = null;
	}

	private boolean validate(DebtlordEntity debtlord) {
		if (pending == null)
			return false;
		if (debtlord.level().getGameTime() >= pending.expirationGameTime || !isValid(debtlord, pending.target)) {
			pending = null;
			return false;
		}
		return true;
	}

	private static boolean isValid(DebtlordEntity debtlord, LivingEntity target) {
		return debtlord.isAlive()
			&& !debtlord.isAltarIntroOrDismissalActive()
			&& !debtlord.isTouchingWaterForBossLogic()
			&& target != null
			&& target != debtlord
			&& target.isAlive()
			&& !target.isRemoved()
			&& target.level() == debtlord.level()
			&& !DebtlordEntity.isEntityTouchingWater(target)
			&& !debtlord.isAlliedTo(target)
			&& !TimothatysTrinketsStunHelper.isMechanicallyImmunePlayer(target);
	}

	public record Entry(
		LivingEntity target,
		long notBeforeGameTime,
		long expirationGameTime,
		Reason reason
	) {
	}
}
