package net.timothaty.timothatystrinkets.mechanics.natures_barrier;

public final class NaturesBarrierTuning {
	public static final float ACTIVATION_HEALTH_RATIO = 0.35F;
	public static final int DURATION_TICKS = 16 * 10;
	public static final int PAGANS_CHARM_CHARGE_COST = 125;
	public static final int PAGANS_CHARM_COOLDOWN_TICKS = 40 * 20;

	public static final float BASE_ABSORPTION = 10.0F;
	public static final float ABSORPTION_PER_EXTRA_LEVEL = 5.0F;

	public static final int OVERLAY_COLOR = 0xB8B8FFE1;
	public static final int OVERLAY_FADE_OUT_TICKS = 10;
	public static final float OVERLAY_SCROLL_SPEED = 0.012F;

	public static final String NBT_REMAINING_ABSORPTION = "TimothatysTrinketsNaturesBarrierRemainingAbsorption";
	public static final String NBT_HURT_SOUND_UNTIL = "TimothatysTrinketsNaturesBarrierHurtSoundUntil";

	private NaturesBarrierTuning() {
	}
}
