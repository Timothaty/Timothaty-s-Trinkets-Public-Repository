package net.timothaty.timothatystrinkets.mechanics.striker_acquisition;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class StrikerForgingEffects {
	private static final double THROW_HORIZONTAL_SPEED = 0.22D;
	private static final double THROW_VERTICAL_SPEED = 0.20D;

	private StrikerForgingEffects() {
	}

	public static Vec3 runForgeImpulse(ServerLevel level, Villager villager, BlockPos grindstonePos, int forgeTick) {
		if (forgeTick == 0)
			StrikerCommissionData.showVisualItem(villager, new ItemStack(Items.IRON_INGOT));
		else if (forgeTick == 120)
			StrikerCommissionData.showVisualItem(villager, new ItemStack(Items.IRON_NUGGET));

		int particleCount = forgeTick == 0 ? 1 + level.getRandom().nextInt(3) : 2 + level.getRandom().nextInt(2);
		level.playSound(
				null,
				grindstonePos,
				SoundEvents.GRINDSTONE_USE,
				SoundSource.BLOCKS,
				0.85F,
				0.9F + level.getRandom().nextFloat() * 0.2F
		);
		villager.swing(InteractionHand.MAIN_HAND);
		spawnForgeSparks(level, grindstonePos, particleCount);
		return randomLookTarget(level, grindstonePos);
	}

	public static void showFinishedStriker(Villager villager) {
		StrikerCommissionData.ensureDeliveryVisual(villager);
	}

	public static void finishForging(ServerLevel level, BlockPos grindstonePos) {
		level.playSound(
				null,
				grindstonePos,
				SoundEvents.ANVIL_LAND,
				SoundSource.BLOCKS,
				0.85F,
				1.3F + level.getRandom().nextFloat() * 0.3F
		);
	}

	public static boolean throwStriker(ServerLevel level, Villager villager, ServerPlayer recipient) {
		Vec3 direction = recipient.position().subtract(villager.position()).multiply(1.0D, 0.0D, 1.0D);
		if (direction.lengthSqr() > 1.0E-6D)
			direction = direction.normalize();
		else {
			direction = villager.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
			direction = direction.lengthSqr() > 1.0E-6D ? direction.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
		}

		ItemEntity item = new ItemEntity(
				level,
				villager.getX(),
				villager.getY() + villager.getBbHeight() * 0.65D,
				villager.getZ(),
				new ItemStack(TimothatysTrinketsModItems.STRIKER_OF_THE_MORNING_STAR.get())
		);
		item.setPickUpDelay(15);
		item.setDeltaMovement(direction.x * THROW_HORIZONTAL_SPEED, THROW_VERTICAL_SPEED, direction.z * THROW_HORIZONTAL_SPEED);
		if (!level.addFreshEntity(item))
			return false;

		level.playSound(
				null,
				villager.blockPosition(),
				TimothatysTrinketsModSounds.RARE_ITEM_DROP_VILLAGER.get(),
				SoundSource.NEUTRAL,
				1.0F,
				0.8F + level.getRandom().nextFloat() * 0.5F
		);
		level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.0F);
		level.sendParticles(
				ParticleTypes.HAPPY_VILLAGER,
				villager.getX(),
				villager.getY() + villager.getBbHeight() * 0.65D,
				villager.getZ(),
				8,
				0.45D,
				0.45D,
				0.45D,
				0.02D
		);
		return true;
	}

	private static Vec3 randomLookTarget(ServerLevel level, BlockPos grindstonePos) {
		return Vec3.atCenterOf(grindstonePos).add(
				-0.18D + level.getRandom().nextDouble() * 0.36D,
				-0.08D + level.getRandom().nextDouble() * 0.23D,
				-0.18D + level.getRandom().nextDouble() * 0.36D
		);
	}

	private static void spawnForgeSparks(ServerLevel level, BlockPos grindstonePos, int count) {
		RandomSource random = level.getRandom();
		for (int index = 0; index < count; index++) {
			double x = grindstonePos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.18D;
			double y = grindstonePos.getY() + 0.80D + (random.nextDouble() - 0.5D) * 0.08D;
			double z = grindstonePos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.18D;
			double velocityX = -0.08D + random.nextDouble() * 0.16D;
			double velocityY = 0.08D + random.nextDouble() * 0.10D;
			double velocityZ = -0.08D + random.nextDouble() * 0.16D;
			level.sendParticles(
					TimothatysTrinketsModParticleTypes.SPARK.get(),
					x,
					y,
					z,
					0,
					velocityX,
					velocityY,
					velocityZ,
					1.0D
			);
		}
	}
}
