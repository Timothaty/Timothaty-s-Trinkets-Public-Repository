package net.timothaty.timothatystrinkets.mechanics.anathema;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;

public final class AnathemaIronGolemTargetGoal extends NearestAttackableTargetGoal<Player> {
	private static final double CLOSE_AGGRO_RADIUS = 6.0D;
	private static final double CLOSE_AGGRO_RADIUS_SQR = CLOSE_AGGRO_RADIUS * CLOSE_AGGRO_RADIUS;

	private final IronGolem golem;

	public AnathemaIronGolemTargetGoal(IronGolem golem) {
		super(golem, Player.class, 5, true, false, candidate -> candidate instanceof Player player && canTarget(golem, player));
		this.golem = golem;
	}

	@Override
	public boolean canContinueToUse() {
		return this.target instanceof Player player && canTarget(golem, player) && super.canContinueToUse();
	}

	private static boolean canTarget(IronGolem golem, Player player) {
		if (player == null || !player.isAlive() || player.isCreative() || player.isSpectator())
			return false;

		int level = AnathemaHelper.getLevel(player);
		if (level < 3)
			return false;
		if (level == 3)
			return golem.distanceToSqr(player) <= CLOSE_AGGRO_RADIUS_SQR;

		return player.level() instanceof ServerLevel serverLevel && AnathemaVillageRules.isVillageTerritory(serverLevel, player.blockPosition());
	}
}
