package net.timothaty.timothatystrinkets.mechanics.blight;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;

public final class BlightGroundEffectHelper {
	private static final String NBT_PUTREFACTION_STANDING_TICKS = "tt_blight_putrefaction_standing_ticks";
	private static final String NBT_GROUND_STATE = "tt_blight_ground_state";
	private static final String NBT_LAST_TICK = "tt_blight_ground_last_tick";
	private static final String NBT_UNDEAD_REGEN_TICKS = "tt_blight_undead_regen_ticks";

	private static final ResourceLocation UNDEAD_ARMOR_ID =
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "blight_undead_armor");
	private static final ResourceLocation UNDEAD_ATTACK_DAMAGE_ID =
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "blight_undead_attack_damage");
	private static final ResourceLocation UNDEAD_MAX_HEALTH_ID =
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "blight_undead_max_health");
	private static final ResourceLocation UNDEAD_MOVEMENT_SPEED_ID =
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "blight_undead_movement_speed");
	private static final ResourceLocation LIVING_MOVEMENT_SPEED_ID =
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "blight_living_movement_speed");

	private BlightGroundEffectHelper() {
	}

	public static void tick(LivingEntity living, boolean inBlightAura, boolean standingOnBlight) {
		GroundState previousState = readState(living);
		GroundState currentState = determineState(living, inBlightAura);

		if (!currentState.isActive()) {
			if (previousState.isActive()) {
				transitionState(living, previousState, currentState);
			}
			if (previousState != GroundState.UNKNOWN) {
				clearGroundTracking(living);
			}
			return;
		}

		int elapsedTicks = updateElapsedTicks(living);

		if (previousState != currentState) {
			transitionState(living, previousState, currentState);
			writeState(living, currentState);
		} else {
			ensureStateModifiers(living, currentState);
		}

		if (currentState == GroundState.UNDEAD) {
			tickUndeadRegeneration(living, elapsedTicks);
			resetPutrefactionTimer(living);
		} else if (currentState == GroundState.LIVING) {
			if (standingOnBlight) {
				tickPutrefactionRisk(living, elapsedTicks);
			} else {
				resetPutrefactionTimer(living);
			}
			clampHealth(living);
		}
	}

	private static GroundState determineState(LivingEntity living, boolean inBlightAura) {
		if (!inBlightAura) {
			return GroundState.OUTSIDE;
		}
		if (living.isInvertedHealAndHarm()) {
			return GroundState.UNDEAD;
		}
		return BlightImmunityHelper.isBlightImmune(living) ? GroundState.IMMUNE : GroundState.LIVING;
	}

	private static void transitionState(LivingEntity living, GroundState previousState, GroundState currentState) {
		if (previousState == GroundState.UNKNOWN) {
			clearAll(living);
		} else {
			clearStateModifiers(living, previousState);
		}

		applyStateModifiers(living, currentState);
		if (previousState == GroundState.UNDEAD && currentState != GroundState.UNDEAD) {
			resetUndeadRegenerationTimer(living);
			clampHealth(living);
		}
		if (currentState != GroundState.LIVING) {
			resetPutrefactionTimer(living);
		}
	}

	private static void clearStateModifiers(LivingEntity living, GroundState state) {
		if (state == GroundState.UNDEAD) {
			clearUndeadBonuses(living);
		} else if (state == GroundState.LIVING) {
			clearLivingPenalties(living);
		}
	}

	private static void applyStateModifiers(LivingEntity living, GroundState state) {
		if (state == GroundState.UNDEAD) {
			applyUndeadBonuses(living);
		} else if (state == GroundState.LIVING) {
			applyLivingPenalties(living);
		}
	}

	private static void ensureStateModifiers(LivingEntity living, GroundState state) {
		if (state == GroundState.UNDEAD || state == GroundState.LIVING) {
			applyStateModifiers(living, state);
		}
	}

	private static void applyUndeadBonuses(LivingEntity living) {
		addTransientModifier(living, Attributes.ARMOR, UNDEAD_ARMOR_ID, BlightConfig.GROUND_UNDEAD_ARMOR_BONUS, AttributeModifier.Operation.ADD_VALUE);
		addTransientModifier(living, Attributes.ATTACK_DAMAGE, UNDEAD_ATTACK_DAMAGE_ID, BlightConfig.GROUND_UNDEAD_ATTACK_DAMAGE_BONUS, AttributeModifier.Operation.ADD_VALUE);
		addTransientModifier(living, Attributes.MAX_HEALTH, UNDEAD_MAX_HEALTH_ID, BlightConfig.GROUND_UNDEAD_MAX_HEALTH_BONUS, AttributeModifier.Operation.ADD_VALUE);
		addTransientModifier(living, Attributes.MOVEMENT_SPEED, UNDEAD_MOVEMENT_SPEED_ID, BlightConfig.GROUND_UNDEAD_MOVEMENT_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	private static void applyLivingPenalties(LivingEntity living) {
		addTransientModifier(living, Attributes.MOVEMENT_SPEED, LIVING_MOVEMENT_SPEED_ID, BlightConfig.GROUND_LIVING_MOVEMENT_SPEED_PENALTY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	private static void tickUndeadRegeneration(LivingEntity living, int elapsedTicks) {
		CompoundTag data = living.getPersistentData();
		int ticks = data.getInt(NBT_UNDEAD_REGEN_TICKS) + elapsedTicks;
		if (ticks < BlightConfig.GROUND_UNDEAD_REGEN_INTERVAL_TICKS) {
			data.putInt(NBT_UNDEAD_REGEN_TICKS, ticks);
			return;
		}

		int pulses = ticks / BlightConfig.GROUND_UNDEAD_REGEN_INTERVAL_TICKS;
		data.putInt(NBT_UNDEAD_REGEN_TICKS, ticks % BlightConfig.GROUND_UNDEAD_REGEN_INTERVAL_TICKS);
		if (living.getHealth() < living.getMaxHealth()) {
			living.heal(BlightConfig.GROUND_UNDEAD_REGEN_PER_SECOND * pulses);
		}
	}

	private static void clearAll(LivingEntity living) {
		clearUndeadBonuses(living);
		clearLivingPenalties(living);
	}

	private static void clearUndeadBonuses(LivingEntity living) {
		removeModifier(living, Attributes.ARMOR, UNDEAD_ARMOR_ID);
		removeModifier(living, Attributes.ATTACK_DAMAGE, UNDEAD_ATTACK_DAMAGE_ID);
		removeModifier(living, Attributes.MAX_HEALTH, UNDEAD_MAX_HEALTH_ID);
		removeModifier(living, Attributes.MOVEMENT_SPEED, UNDEAD_MOVEMENT_SPEED_ID);
	}

	private static void clearLivingPenalties(LivingEntity living) {
		removeModifier(living, Attributes.MOVEMENT_SPEED, LIVING_MOVEMENT_SPEED_ID);
	}

	private static void tickPutrefactionRisk(LivingEntity living, int elapsedTicks) {
		if (living.hasEffect(TimothatysTrinketsModMobEffects.PUTREFACTION)) {
			resetPutrefactionTimer(living);
			return;
		}

		CompoundTag data = living.getPersistentData();
		int ticks = data.getInt(NBT_PUTREFACTION_STANDING_TICKS) + elapsedTicks;
		if (ticks < BlightConfig.GROUND_PUTREFACTION_CHECK_INTERVAL_TICKS) {
			data.putInt(NBT_PUTREFACTION_STANDING_TICKS, ticks);
			return;
		}

		int checks = ticks / BlightConfig.GROUND_PUTREFACTION_CHECK_INTERVAL_TICKS;
		data.putInt(NBT_PUTREFACTION_STANDING_TICKS, ticks % BlightConfig.GROUND_PUTREFACTION_CHECK_INTERVAL_TICKS);
		for (int i = 0; i < checks; i++) {
			if (living.getRandom().nextFloat() < BlightConfig.GROUND_PUTREFACTION_CHANCE) {
				living.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.PUTREFACTION, BlightConfig.GROUND_PUTREFACTION_DURATION_TICKS, 0, false, true, true));
				resetPutrefactionTimer(living);
				return;
			}
		}
	}

	private static void resetPutrefactionTimer(LivingEntity living) {
		living.getPersistentData().remove(NBT_PUTREFACTION_STANDING_TICKS);
	}

	private static void resetUndeadRegenerationTimer(LivingEntity living) {
		living.getPersistentData().remove(NBT_UNDEAD_REGEN_TICKS);
	}

	private static int updateElapsedTicks(LivingEntity living) {
		CompoundTag data = living.getPersistentData();
		int now = living.tickCount;
		int last = data.contains(NBT_LAST_TICK) ? data.getInt(NBT_LAST_TICK) : now;
		data.putInt(NBT_LAST_TICK, now);
		return Math.max(1, now - last);
	}

	private static GroundState readState(LivingEntity living) {
		CompoundTag data = living.getPersistentData();
		if (!data.contains(NBT_GROUND_STATE)) {
			return GroundState.UNKNOWN;
		}
		return GroundState.byId(data.getInt(NBT_GROUND_STATE));
	}

	private static void writeState(LivingEntity living, GroundState state) {
		living.getPersistentData().putInt(NBT_GROUND_STATE, state.id);
	}

	private static void clearGroundTracking(LivingEntity living) {
		CompoundTag data = living.getPersistentData();
		data.remove(NBT_GROUND_STATE);
		data.remove(NBT_LAST_TICK);
		data.remove(NBT_PUTREFACTION_STANDING_TICKS);
		data.remove(NBT_UNDEAD_REGEN_TICKS);
	}

	private static void addTransientModifier(LivingEntity living, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = living.getAttribute(attribute);
		if (instance == null || instance.getModifier(id) != null) {
			return;
		}
		instance.addTransientModifier(new AttributeModifier(id, amount, operation));
	}

	private static void removeModifier(LivingEntity living, Holder<Attribute> attribute, ResourceLocation id) {
		AttributeInstance instance = living.getAttribute(attribute);
		if (instance != null && instance.getModifier(id) != null) {
			instance.removeModifier(id);
		}
	}

	private static void clampHealth(LivingEntity living) {
		float max = living.getMaxHealth();
		if (living.getHealth() > max) {
			living.setHealth(max);
		}
	}

	private enum GroundState {
		UNKNOWN(-1),
		OUTSIDE(0),
		UNDEAD(1),
		LIVING(2),
		IMMUNE(3);

		private final int id;

		GroundState(int id) {
			this.id = id;
		}

		private static GroundState byId(int id) {
			for (GroundState state : values()) {
				if (state.id == id) {
					return state;
				}
			}
			return UNKNOWN;
		}

		private boolean isActive() {
			return this == UNDEAD || this == LIVING;
		}
	}
}
