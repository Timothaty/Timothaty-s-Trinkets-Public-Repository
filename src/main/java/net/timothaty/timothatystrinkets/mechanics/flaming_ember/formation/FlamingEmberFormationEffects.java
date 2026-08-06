package net.timothaty.timothatystrinkets.mechanics.flaming_ember.formation;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class FlamingEmberFormationEffects {
	private static final int FAILURE_PARTICLE_COUNT = 10;

	private FlamingEmberFormationEffects() {
	}

	public static void playSuccess(ServerPlayer player, InteractionHand hand) {
		Vec3 handPosition = getHandPosition(player, hand);
		player.serverLevel().playSound(null, handPosition.x, handPosition.y, handPosition.z,
				SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.9F, 1.05F);
	}

	public static void playFailure(ServerPlayer player, InteractionHand hand, ItemStack charcoalStack) {
		ServerLevel level = player.serverLevel();
		Vec3 handPosition = getHandPosition(player, hand);
		spawnCharcoalFragments(level, player, handPosition, charcoalStack);
		level.playSound(null, handPosition.x, handPosition.y, handPosition.z, SoundEvents.BASALT_BREAK,
				SoundSource.PLAYERS, 0.9F, 0.9F + player.getRandom().nextFloat() * 0.15F);
	}

	private static void spawnCharcoalFragments(ServerLevel level, ServerPlayer player, Vec3 position, ItemStack charcoalStack) {
		RandomSource random = player.getRandom();
		ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, charcoalStack);
		Vec3 bodyCenter = new Vec3(player.getX(), position.y, player.getZ());
		Vec3 outward = position.subtract(bodyCenter).multiply(1.0D, 0.0D, 1.0D);
		if (outward.lengthSqr() < 1.0E-5D)
			outward = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
		outward = outward.normalize();

		for (int index = 0; index < FAILURE_PARTICLE_COUNT; index++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			Vec3 spread = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle)).scale(0.45D);
			Vec3 direction = outward.add(spread).normalize();
			double horizontalSpeed = 0.025D + random.nextDouble() * 0.025D;
			double spawnX = position.x + (random.nextDouble() - 0.5D) * 0.12D;
			double spawnY = position.y + (random.nextDouble() - 0.5D) * 0.10D;
			double spawnZ = position.z + (random.nextDouble() - 0.5D) * 0.12D;
			level.sendParticles(particle, spawnX, spawnY, spawnZ, 0,
					direction.x * horizontalSpeed, -0.015D - random.nextDouble() * 0.025D,
					direction.z * horizontalSpeed, 1.0D);
		}
	}

	private static Vec3 getHandPosition(ServerPlayer player, InteractionHand hand) {
		HumanoidArm physicalArm = hand == InteractionHand.MAIN_HAND
				? player.getMainArm()
				: opposite(player.getMainArm());
		double yawRadians = Math.toRadians(player.getYRot());
		Vec3 right = new Vec3(-Math.cos(yawRadians), 0.0D, -Math.sin(yawRadians));
		double side = physicalArm == HumanoidArm.RIGHT ? 1.0D : -1.0D;
		Vec3 forward = player.getLookAngle().normalize().scale(0.32D);
		Vec3 lateral = right.scale(0.36D * side);
		return new Vec3(player.getX(), player.getY() + player.getBbHeight() * 0.62D, player.getZ())
				.add(forward)
				.add(lateral);
	}

	private static HumanoidArm opposite(HumanoidArm arm) {
		return arm == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
	}
}
