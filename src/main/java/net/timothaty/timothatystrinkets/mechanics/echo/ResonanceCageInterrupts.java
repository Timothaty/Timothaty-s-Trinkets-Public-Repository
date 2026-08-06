package net.timothaty.timothatystrinkets.mechanics.echo;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ResonanceCageInterrupts {
	private static final List<ResonanceInterruptHandler> HANDLERS = List.of(
			new CreeperInterruptHandler()
	);

	private ResonanceCageInterrupts() {
	}

	public static void interrupt(LivingEntity entity) {
		if (entity == null) {
			return;
		}

		softInterrupt(entity);
		runHandlers(entity);
	}

	public static void keepInterrupted(LivingEntity entity) {
		if (entity == null) {
			return;
		}

		softInterrupt(entity);
		runHandlers(entity);
	}

	private static void softInterrupt(LivingEntity entity) {
		entity.stopUsingItem();
		entity.setDeltaMovement(Vec3.ZERO);

		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
			mob.setTarget(null);
			mob.setAggressive(false);
		}
	}

	private static void runHandlers(LivingEntity entity) {
		for (ResonanceInterruptHandler handler : HANDLERS) {
			if (handler.canHandle(entity)) {
				handler.interrupt(entity);
			}
		}
	}

	private interface ResonanceInterruptHandler {
		boolean canHandle(LivingEntity entity);

		void interrupt(LivingEntity entity);
	}

	private static final class CreeperInterruptHandler implements ResonanceInterruptHandler {
		@Override
		public boolean canHandle(LivingEntity entity) {
			return entity instanceof Creeper;
		}

		@Override
		public void interrupt(LivingEntity entity) {
			Creeper creeper = (Creeper) entity;
			creeper.setSwellDir(-1);
		}
	}
}
