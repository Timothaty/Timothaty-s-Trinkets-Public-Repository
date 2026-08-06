package net.timothaty.timothatystrinkets.client.vfx.debtlord_chains;

import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.entity.ai.DebtlordChainsGoal;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class DebtlordChainVisualHandler {
	private static final float LAUNCH_TRAVEL_TICKS = 6.0F;
	private static final Map<DebtlordEntity, ChainVisual> ACTIVE_CHAINS = new WeakHashMap<>();

	private DebtlordChainVisualHandler() {
	}

	public static void record(DebtlordEntity entity, List<Vec3> sourcePositions, LivingEntity target, float partialTick) {
		if (sourcePositions.size() < 2 || target == null)
			return;

		Vec3 targetCenter = interpolatedTargetCenter(target, partialTick);
		Vec3 sourceCenter = sourcePositions.get(0).add(sourcePositions.get(1)).scale(0.5D);
		Vec3 direction = targetCenter.subtract(sourceCenter);
		Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
		Vec3 right = horizontal.lengthSqr() < 0.0001D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();
		double wrapOffset = Math.max(0.32D, target.getBbWidth() * 0.5D + 0.12D);

		ChainVisual visual = ACTIVE_CHAINS.computeIfAbsent(entity, ignored -> new ChainVisual());
		visual.firstSource = sourcePositions.get(0);
		visual.secondSource = sourcePositions.get(1);
		visual.firstTarget = targetCenter.add(right.scale(wrapOffset)).add(0.0D, target.getBbHeight() * 0.08D, 0.0D);
		visual.secondTarget = targetCenter.subtract(right.scale(wrapOffset)).add(0.0D, -target.getBbHeight() * 0.08D, 0.0D);
		visual.bound = entity.isChainSuccessAnimationActive();
		visual.boundCenter = targetCenter;
		visual.boundWidth = target.getBbWidth();
		visual.boundHeight = target.getBbHeight();
		visual.launchProgress = launchProgress(entity, partialTick);
		visual.lastRecordedTick = entity.tickCount;
	}

	public static void tick() {
		Iterator<Map.Entry<DebtlordEntity, ChainVisual>> iterator = ACTIVE_CHAINS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<DebtlordEntity, ChainVisual> entry = iterator.next();
			DebtlordEntity entity = entry.getKey();
			ChainVisual visual = entry.getValue();
			if (entity.isRemoved() || !entity.isUsingChains() || entity.tickCount - visual.lastRecordedTick > 2)
				iterator.remove();
		}
	}

	public static void discard(DebtlordEntity entity) {
		ACTIVE_CHAINS.remove(entity);
	}

	public static Collection<ChainVisual> chains() {
		return Collections.unmodifiableCollection(ACTIVE_CHAINS.values());
	}

	private static Vec3 interpolatedTargetCenter(LivingEntity target, float partialTick) {
		double x = Mth.lerp(partialTick, target.xo, target.getX());
		double y = Mth.lerp(partialTick, target.yo, target.getY()) + target.getBbHeight() * 0.52D;
		double z = Mth.lerp(partialTick, target.zo, target.getZ());
		return new Vec3(x, y, z);
	}

	private static float launchProgress(DebtlordEntity entity, float partialTick) {
		if (entity.getChainPhase() != DebtlordEntity.CHAIN_PHASE_CAST)
			return 1.0F;

		float elapsedTicks = DebtlordChainsGoal.CAST_DURATION_TICKS - entity.getChainCastTicks() + partialTick + 1.0F;
		return Mth.clamp((elapsedTicks - DebtlordChainsGoal.RELEASE_TICK) / LAUNCH_TRAVEL_TICKS, 0.0F, 1.0F);
	}

	public static final class ChainVisual {
		private Vec3 firstSource = Vec3.ZERO;
		private Vec3 secondSource = Vec3.ZERO;
		private Vec3 firstTarget = Vec3.ZERO;
		private Vec3 secondTarget = Vec3.ZERO;
		private Vec3 boundCenter = Vec3.ZERO;
		private float boundWidth;
		private float boundHeight;
		private float launchProgress = 1.0F;
		private boolean bound;
		private int lastRecordedTick;

		public Vec3 firstSource() {
			return firstSource;
		}

		public Vec3 secondSource() {
			return secondSource;
		}

		public Vec3 firstTarget() {
			return firstTarget;
		}

		public Vec3 secondTarget() {
			return secondTarget;
		}

		public Vec3 boundCenter() {
			return boundCenter;
		}

		public float boundWidth() {
			return boundWidth;
		}

		public float boundHeight() {
			return boundHeight;
		}

		public boolean bound() {
			return bound;
		}

		public float launchProgress() {
			return launchProgress;
		}
	}
}
