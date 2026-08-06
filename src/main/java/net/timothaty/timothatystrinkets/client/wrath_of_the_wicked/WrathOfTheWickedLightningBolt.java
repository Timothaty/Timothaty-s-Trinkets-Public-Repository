package net.timothaty.timothatystrinkets.client.wrath_of_the_wicked;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class WrathOfTheWickedLightningBolt {
	private static final double MAX_ENDPOINT_DISTANCE_SQR = 4.0D * 4.0D;

	final BoltKind kind;
	final Attachment attachment;
	final Vec3 spawnStart;
	final Vec3 endpoint;
	final Vec3[] path;
	final int branchRootIndex;
	final Vec3[] branch;
	final long spawnGameTime;
	final int lifetimeTicks;
	final boolean preCharge;

	private WrathOfTheWickedLightningBolt(
			BoltKind kind,
			Attachment attachment,
			Vec3 spawnStart,
			Vec3 endpoint,
			Vec3[] path,
			int branchRootIndex,
			Vec3[] branch,
			long spawnGameTime,
			int lifetimeTicks,
			boolean preCharge
	) {
		this.kind = kind;
		this.attachment = attachment;
		this.spawnStart = spawnStart;
		this.endpoint = endpoint;
		this.path = path;
		this.branchRootIndex = branchRootIndex;
		this.branch = branch;
		this.spawnGameTime = spawnGameTime;
		this.lifetimeTicks = lifetimeTicks;
		this.preCharge = preCharge;
	}

	static WrathOfTheWickedLightningBolt create(
			LivingEntity entity,
			long seed,
			long spawnGameTime,
			int generationTick,
			int visualEndTick,
			boolean preCharge
	) {
		RandomSource random = RandomSource.create(seed);
		Attachment attachment = createStandardAttachment(random, preCharge);
		Vec3 start = attachment.position(entity, 1.0F);
		double angle = random.nextDouble() * Math.PI * 2.0D;
		double radius = randomBetween(random, 0.4D, 1.0D);
		double endHeightFraction = Mth.clamp(
				attachment.heightFraction
						+ randomBetween(random, -0.16D, 0.13D),
				0.05D,
				0.58D
		);
		Vec3 endpoint = new Vec3(
				entity.getX() + Math.cos(angle) * radius,
				entity.getY() + entity.getBbHeight() * endHeightFraction,
				entity.getZ() + Math.sin(angle) * radius
		);
		return assemble(
				BoltKind.STANDARD,
				attachment,
				start,
				endpoint,
				6 + random.nextInt(4),
				1.0D,
				preCharge ? 0.0F : 0.30F,
				spawnGameTime,
				preCharge ? 3 : 4,
				preCharge ? 4 : 6,
				visualEndTick - generationTick,
				preCharge,
				random
		);
	}

	static WrathOfTheWickedLightningBolt createTorsoDischarge(
			LivingEntity entity,
			long seed,
			long spawnGameTime
	) {
		RandomSource random = RandomSource.create(seed);
		Attachment attachment = createTorsoAttachment(random);
		Vec3 start = attachment.position(entity, 1.0F);
		Vec3 endpoint = createTorsoEndpoint(entity, random);
		return assemble(
				BoltKind.TORSO_DISCHARGE,
				attachment,
				start,
				endpoint,
				7 + random.nextInt(4),
				1.38D,
				0.20F,
				spawnGameTime,
				3,
				5,
				Integer.MAX_VALUE,
				false,
				random
		);
	}

	boolean isExpiredOrStretched(LivingEntity entity, long gameTime) {
		return gameTime - spawnGameTime >= lifetimeTicks
				|| endpoint.distanceToSqr(entity.position()) > MAX_ENDPOINT_DISTANCE_SQR;
	}

	boolean isStretchedFrom(Vec3 currentStart) {
		return currentStart.distanceToSqr(endpoint) > MAX_ENDPOINT_DISTANCE_SQR;
	}

	float widthScale() {
		return kind == BoltKind.TORSO_DISCHARGE ? 0.83F : 1.0F;
	}

	private static WrathOfTheWickedLightningBolt assemble(
			BoltKind kind,
			Attachment attachment,
			Vec3 start,
			Vec3 endpoint,
			int segmentCount,
			double jitterMultiplier,
			float branchChance,
			long spawnGameTime,
			int minimumLifetimeTicks,
			int maximumLifetimeTicks,
			int lifetimeLimit,
			boolean preCharge,
			RandomSource random
	) {
		Vec3[] path = createPath(
				start,
				endpoint,
				segmentCount,
				jitterMultiplier,
				random
		);
		int branchRootIndex = -1;
		Vec3[] branch = null;
		if (branchChance > 0.0F && random.nextFloat() < branchChance) {
			branchRootIndex = 2 + random.nextInt(path.length - 4);
			branch = createBranch(path[branchRootIndex], random);
		}
		int lifetimeTicks = minimumLifetimeTicks + random.nextInt(
				maximumLifetimeTicks - minimumLifetimeTicks + 1
		);
		lifetimeTicks = Math.max(1, Math.min(lifetimeTicks, lifetimeLimit));
		return new WrathOfTheWickedLightningBolt(
				kind,
				attachment,
				start,
				endpoint,
				path,
				branchRootIndex,
				branch,
				spawnGameTime,
				lifetimeTicks,
				preCharge
		);
	}

	private static Attachment createStandardAttachment(
			RandomSource random,
			boolean preCharge
	) {
		int source = preCharge ? random.nextInt(2) : random.nextInt(5);
		double side = source == 0 || source == 2
				? -1.0D
				: source == 1 || source == 3
						? 1.0D
						: random.nextBoolean() ? 1.0D : -1.0D;
		double heightFraction;
		double lateralFraction;
		if (source <= 1) {
			heightFraction = randomBetween(random, 0.08D, 0.16D);
			lateralFraction = side * randomBetween(random, 0.28D, 0.42D);
		} else if (source <= 3) {
			heightFraction = randomBetween(random, 0.34D, 0.46D);
			lateralFraction = side * randomBetween(random, 0.25D, 0.40D);
		} else {
			heightFraction = randomBetween(random, 0.50D, 0.62D);
			lateralFraction = side * randomBetween(random, 0.12D, 0.30D);
		}
		return new Attachment(
				heightFraction,
				lateralFraction,
				randomBetween(random, -0.10D, 0.10D)
		);
	}

	private static Attachment createTorsoAttachment(RandomSource random) {
		double lateralFraction;
		if (random.nextFloat() < 0.78F) {
			double side = random.nextBoolean() ? 1.0D : -1.0D;
			lateralFraction = side * randomBetween(random, 0.16D, 0.28D);
		} else {
			lateralFraction = randomBetween(random, -0.12D, 0.12D);
		}
		return new Attachment(
				randomBetween(random, 0.75D, 0.80D),
				lateralFraction,
				randomBetween(random, -0.14D, 0.14D)
		);
	}

	private static Vec3 createTorsoEndpoint(
			LivingEntity entity,
			RandomSource random
	) {
		float bodyYaw = entity.yBodyRot;
		double radians = bodyYaw * Mth.DEG_TO_RAD;
		Vec3 right = new Vec3(Math.cos(radians), 0.0D, Math.sin(radians));
		Vec3 forward = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
		double side = random.nextBoolean() ? 1.0D : -1.0D;
		double backwardBias = randomBetween(random, -0.62D, 0.12D);
		Vec3 direction = right.scale(side).add(forward.scale(backwardBias)).normalize();
		double radius = randomBetween(random, 0.7D, 1.2D);

		double heightRoll = random.nextDouble();
		double endpointHeightFraction;
		if (heightRoll < 0.08D) {
			endpointHeightFraction = randomBetween(random, 0.05D, 0.14D);
		} else if (heightRoll < 0.18D) {
			endpointHeightFraction = randomBetween(random, 0.45D, 0.70D);
		} else {
			endpointHeightFraction = randomBetween(random, 0.10D, 0.55D);
		}
		return new Vec3(
				entity.getX() + direction.x * radius,
				entity.getY() + entity.getBbHeight() * endpointHeightFraction,
				entity.getZ() + direction.z * radius
		);
	}

	private static Vec3[] createPath(
			Vec3 start,
			Vec3 endpoint,
			int segmentCount,
			double jitterMultiplier,
			RandomSource random
	) {
		Vec3 line = endpoint.subtract(start);
		double length = Math.max(0.001D, line.length());
		Vec3 direction = line.scale(1.0D / length);
		Vec3 firstAxis = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (firstAxis.lengthSqr() < 1.0E-6D)
			firstAxis = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
		firstAxis = firstAxis.normalize();
		Vec3 secondAxis = direction.cross(firstAxis).normalize();
		double jitter = Math.min(0.16D, 0.045D + length * 0.085D)
				* jitterMultiplier;

		Vec3[] points = new Vec3[segmentCount + 1];
		points[0] = start;
		points[segmentCount] = endpoint;
		for (int index = 1; index < segmentCount; index++) {
			double progress = index / (double) segmentCount;
			double envelope = Math.sin(Math.PI * progress);
			Vec3 base = start.lerp(endpoint, progress);
			points[index] = base
					.add(firstAxis.scale(
							randomBetween(random, -jitter, jitter) * envelope
					))
					.add(secondAxis.scale(
							randomBetween(random, -jitter, jitter) * envelope
					));
		}
		return points;
	}

	private static Vec3[] createBranch(Vec3 root, RandomSource random) {
		Vec3 direction = new Vec3(
				randomBetween(random, -1.0D, 1.0D),
				randomBetween(random, -0.65D, 0.85D),
				randomBetween(random, -1.0D, 1.0D)
		);
		if (direction.lengthSqr() < 1.0E-6D)
			direction = new Vec3(1.0D, 0.25D, 0.0D);
		direction = direction.normalize();
		double length = randomBetween(random, 0.14D, 0.30D);
		Vec3 endpoint = root.add(direction.scale(length));
		Vec3 middle = root.lerp(endpoint, 0.52D).add(
				randomBetween(random, -0.035D, 0.035D),
				randomBetween(random, -0.025D, 0.045D),
				randomBetween(random, -0.035D, 0.035D)
		);
		return new Vec3[]{root, middle, endpoint};
	}

	private static double randomBetween(
			RandomSource random,
			double minimum,
			double maximum
	) {
		return minimum + random.nextDouble() * (maximum - minimum);
	}

	enum BoltKind {
		STANDARD,
		TORSO_DISCHARGE
	}

	record Attachment(
			double heightFraction,
			double lateralWidthFraction,
			double forwardWidthFraction
	) {
		Vec3 position(LivingEntity entity, float partialTick) {
			double x = Mth.lerp(partialTick, entity.xo, entity.getX());
			double y = Mth.lerp(partialTick, entity.yo, entity.getY())
					+ entity.getBbHeight() * heightFraction;
			double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
			float bodyYaw = Mth.rotLerp(
					partialTick,
					entity.yBodyRotO,
					entity.yBodyRot
			);
			double radians = bodyYaw * Mth.DEG_TO_RAD;
			double width = Math.max(0.1D, entity.getBbWidth());
			double lateral = lateralWidthFraction * width;
			double forward = forwardWidthFraction * width;
			return new Vec3(
					x + Math.cos(radians) * lateral - Math.sin(radians) * forward,
					y,
					z + Math.sin(radians) * lateral + Math.cos(radians) * forward
			);
		}
	}
}
