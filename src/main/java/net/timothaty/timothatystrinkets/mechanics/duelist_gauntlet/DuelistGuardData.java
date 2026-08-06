package net.timothaty.timothatystrinkets.mechanics.duelist_gauntlet;

public final class DuelistGuardData {
	public static final String NBT_GUARDING = "TimothatysTrinketsDuelistGuarding";
	public static final String NBT_DIRECTION = "TimothatysTrinketsDuelistGuardDirection";
	public static final String NBT_STAMINA = "TimothatysTrinketsDuelistGuardStamina";
	public static final String NBT_BREAK_COOLDOWN = "TimothatysTrinketsDuelistGuardBreakCooldown";
	public static final String NBT_REGEN_DELAY = "TimothatysTrinketsDuelistGuardRegenDelay";

	public static final float MAX_STAMINA = 100.0F;
	public static final float STAMINA_REGEN_PER_TICK = 4.0F;
	public static final int STAMINA_REGEN_DELAY_TICKS = 40;
	public static final float CENTER_PARRY_STAMINA_COST_MULTIPLIER = 2.0F;
	public static final float SIDE_DEFLECT_STAMINA_COST = 25.0F;
	public static final float RIPOSTE_STAMINA_GAIN = 30.0F;
	public static final int GUARD_BREAK_COOLDOWN_TICKS = 40;
	public static final int GUARD_BREAK_EFFECT_TICKS = 60;
	public static final int GUARD_BREAK_SLOWNESS_AMPLIFIER = 2;
	public static final int GUARD_BREAK_FATIGUE_AMPLIFIER = 1;
	public static final double FRONTAL_GUARD_ARC_DEGREES = 100.0D;
	public static final float SIDE_DEFLECT_BLOCK_RATIO = 0.50F;
	public static final float SIDE_DEFLECT_BOSS_BLOCK_RATIO = 0.20F;
	public static final int SIDE_DEFLECT_STUN_TICKS = 40;
	public static final int SIDE_DEFLECT_PLAYER_STUN_TICKS = 20;
	public static final int SIDE_DEFLECT_RIPOSTE_TICKS = 40;
	public static final int SIDE_DEFLECT_WEAPON_COOLDOWN_TICKS = 7 * 20;
	public static final double SIDE_DEFLECT_KNOCKBACK = 0.65D;
	public static final int CENTER_PARRY_GAUNTLET_DURABILITY_COST = 2;
	public static final int SIDE_DEFLECT_GAUNTLET_DURABILITY_COST = 3;
	public static final int BOSS_GAUNTLET_DURABILITY_MULTIPLIER = 10;
	public static final float RIPOSTE_DAMAGE_MULTIPLIER = 1.75F;
	public static final int DEBUG_ACTIONBAR_INTERVAL_TICKS = 10;
	public static final int STAMINA_SYNC_INTERVAL_TICKS = 10;

	public static final float CLIENT_YAW_DEADZONE_DEGREES = 0.7F;
	public static final float CLIENT_GUARD_CURSOR_YAW_SCALE = 0.08F;
	public static final float CLIENT_GUARD_CURSOR_RETURN_STEP = 0.025F;
	public static final float CLIENT_GUARD_CURSOR_SIDE_ENTER = 0.35F;
	public static final float CLIENT_GUARD_CURSOR_CENTER_ENTER = 0.24F;
	public static final int CLIENT_GUARD_SWITCH_COOLDOWN_TICKS = 5;

	private DuelistGuardData() {
	}
}
