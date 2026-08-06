package net.timothaty.timothatystrinkets.mechanics.stun;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.util.TimothatysTrinketsStunHelper;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class StunnedMovementController {
	private static final String NBT_ANCHOR_X = "ttr_stun_anchor_x";
	private static final String NBT_ANCHOR_Y = "ttr_stun_anchor_y";
	private static final String NBT_ANCHOR_Z = "ttr_stun_anchor_z";
	private static final String NBT_ANCHOR_VALID = "ttr_stun_anchor_valid";
	private static final String NBT_LAST_HURT_TIME = "ttr_stun_last_hurt_time";
	private static final String NBT_KNOCKBACK_GRACE_UNTIL = "ttr_stun_knockback_grace_until";
	private static final int KNOCKBACK_GRACE_TICKS = 4;

	private StunnedMovementController() {
	}

	@SubscribeEvent
	public static void onEntityTickPost(EntityTickEvent.Post event) {
		Entity rawEntity = event.getEntity();
		if (!(rawEntity instanceof LivingEntity living))
			return;
		if (living.level().isClientSide())
			return;
		if (!TimothatysTrinketsStunHelper.isStunned(living)) {
			if (living.getPersistentData().getBoolean(NBT_ANCHOR_VALID)) {
				clearAnchor(living);
			}
			return;
		}

		freeze(living);
	}

	public static void freeze(LivingEntity living) {
		if (living == null)
			return;

		CompoundTag data = living.getPersistentData();
		if (!data.getBoolean(NBT_ANCHOR_VALID)) {
			setAnchor(living, data);
		}

		updateKnockbackGrace(living, data);
		boolean allowKnockback = living.level().getGameTime() <= data.getLong(NBT_KNOCKBACK_GRACE_UNTIL);

		if (allowKnockback) {
			setAnchor(living, data);
		} else if (living instanceof Mob) {
			living.setPos(data.getDouble(NBT_ANCHOR_X), living.getY(), data.getDouble(NBT_ANCHOR_Z));
		}

		double yMotion = Math.min(0.0D, living.getDeltaMovement().y);
		if (allowKnockback) {
			living.setDeltaMovement(living.getDeltaMovement().x * 0.82D, yMotion, living.getDeltaMovement().z * 0.82D);
		} else {
			living.setDeltaMovement(0.0D, yMotion, 0.0D);
		}

		living.hasImpulse = true;
		living.hurtMarked = true;

		if (living instanceof Mob mob) {
			mob.getNavigation().stop();
		}
	}

	public static void clearAnchor(LivingEntity living) {
		if (living == null)
			return;

		CompoundTag data = living.getPersistentData();
		data.remove(NBT_ANCHOR_X);
		data.remove(NBT_ANCHOR_Y);
		data.remove(NBT_ANCHOR_Z);
		data.remove(NBT_ANCHOR_VALID);
		data.remove(NBT_LAST_HURT_TIME);
		data.remove(NBT_KNOCKBACK_GRACE_UNTIL);
	}

	public static void clearMobCombatState(Mob mob) {
		if (mob == null)
			return;

		mob.getNavigation().stop();
		mob.setTarget(null);
		mob.setLastHurtByMob(null);
		mob.setAggressive(false);
	}

	private static void updateKnockbackGrace(LivingEntity living, CompoundTag data) {
		int previousHurtTime = data.getInt(NBT_LAST_HURT_TIME);
		if (living.hurtTime > previousHurtTime) {
			data.putLong(NBT_KNOCKBACK_GRACE_UNTIL, living.level().getGameTime() + KNOCKBACK_GRACE_TICKS);
		}

		data.putInt(NBT_LAST_HURT_TIME, living.hurtTime);
	}

	private static void setAnchor(LivingEntity living, CompoundTag data) {
		data.putDouble(NBT_ANCHOR_X, living.getX());
		data.putDouble(NBT_ANCHOR_Y, living.getY());
		data.putDouble(NBT_ANCHOR_Z, living.getZ());
		data.putBoolean(NBT_ANCHOR_VALID, true);
	}
}
