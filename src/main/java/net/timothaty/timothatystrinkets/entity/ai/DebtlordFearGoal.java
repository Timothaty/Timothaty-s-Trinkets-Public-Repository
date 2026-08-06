package net.timothaty.timothatystrinkets.entity.ai;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;
import net.timothaty.timothatystrinkets.network.DebtlordFearCameraShakeMessage;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public final class DebtlordFearGoal extends Goal {
	public static final int CAST_DURATION_TICKS = 2 * 20;
	public static final int FEAR_START_TICK = 25;
	public static final int FEAR_END_TICK = 35;
	public static final int COOLDOWN_TICKS = 9 * 20;
	public static final int REQUIRED_HITS = 3;
	public static final int HIT_STREAK_WINDOW_TICKS = 3 * 20;
	public static final int FEAR_EFFECT_DURATION_TICKS = 5 * 20;
	public static final double TUNNEL_LENGTH = 6.0D;
	public static final double TUNNEL_WIDTH = 3.0D;
	private static final double TUNNEL_HALF_WIDTH = TUNNEL_WIDTH * 0.5D;
	private static final double TUNNEL_HEIGHT_BELOW = 0.75D;
	private static final double TUNNEL_HEIGHT_ABOVE = 3.5D;
	private static final int ROAR_PARTICLES_PER_TICK = 4;
	private static final double MODEL_UNIT = 1.0D / 16.0D;
	private static final double MOUTH_Y_OFFSET = 40.4D * MODEL_UNIT;
	private static final double MOUTH_FORWARD_OFFSET = 4.0D * MODEL_UNIT;
	private static final double ROAR_PARTICLE_SPEED = 0.36D;

	private final DebtlordEntity debtlord;

	public DebtlordFearGoal(DebtlordEntity debtlord) {
		this.debtlord = debtlord;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return debtlord.canStartFearAbility();
	}

	@Override
	public boolean canContinueToUse() {
		return debtlord.isAlive() && debtlord.isUsingFear();
	}

	@Override
	public void start() {
		LivingEntity target = debtlord.getFearTriggerTarget();
		if (target == null)
			return;

		debtlord.startFearCast(target, CAST_DURATION_TICKS);
	}

	@Override
	public void tick() {
		debtlord.trackFearTarget();
	}

	@Override
	public void stop() {
		debtlord.finishFearCast();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	public static void performFearPulse(DebtlordEntity debtlord) {
		if (!(debtlord.level() instanceof ServerLevel serverLevel))
			return;

		TunnelFrame tunnel = createTunnelFrame(debtlord);
		spawnRoarParticles(serverLevel, tunnel);
		Vec3 end = tunnel.origin.add(tunnel.forward.scale(TUNNEL_LENGTH));
		AABB searchBounds = new AABB(tunnel.origin, end).inflate(TUNNEL_HALF_WIDTH, TUNNEL_HEIGHT_ABOVE, TUNNEL_HALF_WIDTH);
		List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBounds, target -> isValidFearTarget(debtlord, target));
		for (LivingEntity target : targets) {
			if (!isInsideTunnel(tunnel, target) || target.hasEffect(TimothatysTrinketsModMobEffects.FEAR))
				continue;

			target.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.FEAR, FEAR_EFFECT_DURATION_TICKS, 0, false, false, true), debtlord);
			if (target instanceof ServerPlayer player)
				PacketDistributor.sendToPlayer(player, DebtlordFearCameraShakeMessage.INSTANCE);
		}
	}

	public static void playRoar(DebtlordEntity debtlord) {
		debtlord.level().playSound(null, debtlord.blockPosition(), TimothatysTrinketsModSounds.DEBTLORD_ROAR.get(), SoundSource.HOSTILE, 1.45F, 1.0F);
	}

	private static boolean isValidFearTarget(DebtlordEntity debtlord, LivingEntity target) {
		if (target == debtlord || !target.isAlive() || debtlord.isAlliedTo(target))
			return false;
		return !(target instanceof Player player) || (!player.isCreative() && !player.isSpectator());
	}

	private static boolean isInsideTunnel(TunnelFrame tunnel, LivingEntity target) {
		Vec3 relative = target.position().subtract(tunnel.origin);
		double forwardDistance = relative.dot(tunnel.forward);
		double lateralDistance = Math.abs(relative.dot(tunnel.right));
		double feetY = target.getBoundingBox().minY;
		return forwardDistance >= 0.0D
			&& forwardDistance <= TUNNEL_LENGTH
			&& lateralDistance <= TUNNEL_HALF_WIDTH
			&& feetY >= tunnel.origin.y - TUNNEL_HEIGHT_BELOW
			&& feetY <= tunnel.origin.y + TUNNEL_HEIGHT_ABOVE;
	}

	private static void spawnRoarParticles(ServerLevel serverLevel, TunnelFrame tunnel) {
		Vec3 mouth = tunnel.mouth;
		for (int i = 0; i < ROAR_PARTICLES_PER_TICK; i++) {
			double lateralDistance = (serverLevel.getRandom().nextDouble() - 0.5D) * 0.22D;
			double verticalDistance = (serverLevel.getRandom().nextDouble() - 0.5D) * 0.12D;
			double forwardDistance = serverLevel.getRandom().nextDouble() * 0.08D;
			Vec3 pos = mouth
				.add(tunnel.forward.scale(forwardDistance))
				.add(tunnel.right.scale(lateralDistance))
				.add(0.0D, verticalDistance, 0.0D);
			double size = 0.48D + serverLevel.getRandom().nextDouble() * 0.34D;
			serverLevel.sendParticles(
				TimothatysTrinketsModParticleTypes.ROAR_OF_FEAR.get(),
				pos.x, pos.y, pos.z,
				0,
				tunnel.forward.x * ROAR_PARTICLE_SPEED, size, tunnel.forward.z * ROAR_PARTICLE_SPEED,
				1.0D
			);
		}
	}

	private static TunnelFrame createTunnelFrame(DebtlordEntity debtlord) {
		Vec3 look = debtlord.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0.0D, look.z).normalize();
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		Vec3 origin = debtlord.position().add(forward.scale(debtlord.getBbWidth() * 0.5D));
		Vec3 mouth = debtlord.position()
			.add(0.0D, MOUTH_Y_OFFSET, 0.0D)
			.add(forward.scale(MOUTH_FORWARD_OFFSET));
		return new TunnelFrame(origin, mouth, forward, right);
	}

	private record TunnelFrame(Vec3 origin, Vec3 mouth, Vec3 forward, Vec3 right) {
	}
}
