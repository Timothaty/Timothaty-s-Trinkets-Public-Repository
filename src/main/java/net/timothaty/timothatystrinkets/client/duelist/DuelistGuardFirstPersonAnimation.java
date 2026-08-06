package net.timothaty.timothatystrinkets.client.duelist;

import net.timothaty.timothatystrinkets.client.DuelistGuardClient;
import net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet.DuelistGuardDirection;

import net.minecraft.util.Mth;

public final class DuelistGuardFirstPersonAnimation {
	private static final float ACTIVE_STEP = 0.24F;
	private static final float SIDE_STEP = 0.30F;

	private static DuelistGuardDirection previousVisualDirection = DuelistGuardDirection.NONE;
	private static DuelistGuardDirection currentVisualDirection = DuelistGuardDirection.NONE;
	private static float previousActive;
	private static float currentActive;
	private static float previousSide;
	private static float currentSide;

	private DuelistGuardFirstPersonAnimation() {
	}

	public static void tick() {
		previousActive = currentActive;
		previousSide = currentSide;

		DuelistGuardDirection targetDirection = DuelistGuardClient.getVisualGuardDirection();
		if (!targetDirection.canBeHeldByPlayer()) {
			targetDirection = DuelistGuardDirection.NONE;
		}

		if (targetDirection != currentVisualDirection) {
			previousVisualDirection = currentVisualDirection;
			currentVisualDirection = targetDirection;
		}

		float targetActive = targetDirection.canBeHeldByPlayer() ? 1.0F : 0.0F;
		currentActive = Mth.approach(currentActive, targetActive, ACTIVE_STEP);
		currentSide = Mth.approach(currentSide, sideFor(targetDirection), SIDE_STEP);
	}

	public static VisualPose sample(float partialTick) {
		return new VisualPose(
				Mth.lerp(partialTick, previousActive, currentActive),
				Mth.lerp(partialTick, previousSide, currentSide),
				previousVisualDirection,
				currentVisualDirection
		);
	}

	public static void reset() {
		previousVisualDirection = DuelistGuardDirection.NONE;
		currentVisualDirection = DuelistGuardDirection.NONE;
		previousActive = 0.0F;
		currentActive = 0.0F;
		previousSide = 0.0F;
		currentSide = 0.0F;
	}

	private static float sideFor(DuelistGuardDirection direction) {
		return switch (direction) {
			case LEFT -> -1.0F;
			case RIGHT -> 1.0F;
			default -> 0.0F;
		};
	}

	public record VisualPose(float active, float side, DuelistGuardDirection previousDirection, DuelistGuardDirection currentDirection) {
		public boolean visible() {
			return active > 0.02F;
		}
	}
}
