package net.timothaty.timothatystrinkets.client.wrath_of_the_wicked;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
final class WrathOfTheWickedLightningGeometry {
	private static final float OUTER_WIDTH = 0.048F;
	private static final float MAIN_WIDTH = 0.030F;
	private static final float CORE_WIDTH = 0.013F;

	private static final int OUTER_RED = 0x4B;
	private static final int OUTER_GREEN = 0x03;
	private static final int OUTER_BLUE = 0x0D;
	private static final int MAIN_RED = 0xA5;
	private static final int MAIN_GREEN = 0x0A;
	private static final int MAIN_BLUE = 0x2C;
	private static final int CORE_RED = 0xFF;
	private static final int CORE_GREEN = 0xD0;
	private static final int CORE_BLUE = 0xDA;

	private WrathOfTheWickedLightningGeometry() {
	}

	static void render(
			Matrix4f matrix,
			VertexConsumer consumer,
			LivingEntity entity,
			WrathOfTheWickedLightningBolt bolt,
			long gameTime,
			float partialTick,
			float forcedFade
	) {
		float age = gameTime - bolt.spawnGameTime + partialTick;
		if (age < 0.0F || age >= bolt.lifetimeTicks)
			return;

		float progress = Mth.clamp(age / bolt.lifetimeTicks, 0.0F, 1.0F);
		float fade = smoothstep(0.0F, 0.15F, progress)
				* (1.0F - smoothstep(0.60F, 1.0F, progress))
				* forcedFade;
		if (fade <= 0.002F)
			return;

		float endShrink = 1.0F - 0.28F * smoothstep(0.55F, 1.0F, progress);
		float preChargeScale = bolt.preCharge ? 0.72F : 1.0F;
		float widthScale = bolt.widthScale();
		Vec3 currentStart = bolt.attachment.position(entity, partialTick);
		if (bolt.isStretchedFrom(currentStart))
			return;
		Vec3 startDelta = currentStart.subtract(bolt.spawnStart);

		renderLayer(
				matrix,
				consumer,
				bolt,
				startDelta,
				OUTER_WIDTH * endShrink * preChargeScale * widthScale,
				OUTER_RED,
				OUTER_GREEN,
				OUTER_BLUE,
				fade * preChargeScale
		);
		renderLayer(
				matrix,
				consumer,
				bolt,
				startDelta,
				MAIN_WIDTH * endShrink * preChargeScale * widthScale,
				MAIN_RED,
				MAIN_GREEN,
				MAIN_BLUE,
				fade * preChargeScale
		);
		renderLayer(
				matrix,
				consumer,
				bolt,
				startDelta,
				CORE_WIDTH * endShrink * preChargeScale * widthScale,
				CORE_RED,
				CORE_GREEN,
				CORE_BLUE,
				fade * preChargeScale
		);
	}

	private static void renderLayer(
			Matrix4f matrix,
			VertexConsumer consumer,
			WrathOfTheWickedLightningBolt bolt,
			Vec3 startDelta,
			float width,
			int red,
			int green,
			int blue,
			float alpha
	) {
		renderPath(
				matrix,
				consumer,
				bolt.path,
				startDelta,
				width,
				red,
				green,
				blue,
				alpha
		);
		if (bolt.branch == null)
			return;

		Vec3 currentRoot = adjustedPoint(
				bolt.path,
				bolt.branchRootIndex,
				startDelta
		);
		Vec3 branchRootDelta = currentRoot.subtract(bolt.branch[0]);
		renderPath(
				matrix,
				consumer,
				bolt.branch,
				branchRootDelta,
				width * 0.72F,
				red,
				green,
				blue,
				alpha
		);
	}

	private static void renderPath(
			Matrix4f matrix,
			VertexConsumer consumer,
			Vec3[] points,
			Vec3 movingRootDelta,
			float width,
			int red,
			int green,
			int blue,
			float alpha
	) {
		for (int index = 0; index < points.length - 1; index++) {
			renderPrismSegment(
					matrix,
					consumer,
					adjustedPoint(points, index, movingRootDelta),
					adjustedPoint(points, index + 1, movingRootDelta),
					width * 0.5F,
					red,
					green,
					blue,
					alpha
			);
		}
	}

	private static Vec3 adjustedPoint(
			Vec3[] points,
			int index,
			Vec3 movingRootDelta
	) {
		double progress = index / (double) (points.length - 1);
		return points[index].add(movingRootDelta.scale(1.0D - progress));
	}

	private static void renderPrismSegment(
			Matrix4f matrix,
			VertexConsumer consumer,
			Vec3 start,
			Vec3 end,
			double radius,
			int red,
			int green,
			int blue,
			float alpha
	) {
		Vec3 direction = end.subtract(start);
		if (direction.lengthSqr() < 1.0E-8D)
			return;
		direction = direction.normalize();
		Vec3 firstAxis = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (firstAxis.lengthSqr() < 1.0E-6D)
			firstAxis = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
		firstAxis = firstAxis.normalize().scale(radius);
		Vec3 secondAxis = direction.cross(firstAxis).normalize().scale(radius);

		Vec3 startA = start.add(firstAxis).add(secondAxis);
		Vec3 startB = start.add(firstAxis).subtract(secondAxis);
		Vec3 startC = start.subtract(firstAxis).subtract(secondAxis);
		Vec3 startD = start.subtract(firstAxis).add(secondAxis);
		Vec3 endA = end.add(firstAxis).add(secondAxis);
		Vec3 endB = end.add(firstAxis).subtract(secondAxis);
		Vec3 endC = end.subtract(firstAxis).subtract(secondAxis);
		Vec3 endD = end.subtract(firstAxis).add(secondAxis);

		quad(matrix, consumer, startA, endA, endB, startB, red, green, blue, alpha);
		quad(matrix, consumer, startB, endB, endC, startC, red, green, blue, alpha);
		quad(matrix, consumer, startC, endC, endD, startD, red, green, blue, alpha);
		quad(matrix, consumer, startD, endD, endA, startA, red, green, blue, alpha);
	}

	private static void quad(
			Matrix4f matrix,
			VertexConsumer consumer,
			Vec3 first,
			Vec3 second,
			Vec3 third,
			Vec3 fourth,
			int red,
			int green,
			int blue,
			float alpha
	) {
		vertex(matrix, consumer, first, red, green, blue, alpha);
		vertex(matrix, consumer, second, red, green, blue, alpha);
		vertex(matrix, consumer, third, red, green, blue, alpha);
		vertex(matrix, consumer, fourth, red, green, blue, alpha);
	}

	private static void vertex(
			Matrix4f matrix,
			VertexConsumer consumer,
			Vec3 position,
			int red,
			int green,
			int blue,
			float alpha
	) {
		consumer.addVertex(
				matrix,
				(float) position.x,
				(float) position.y,
				(float) position.z
		).setColor(
				red / 255.0F,
				green / 255.0F,
				blue / 255.0F,
				Mth.clamp(alpha, 0.0F, 1.0F)
		);
	}

	private static float smoothstep(float edge0, float edge1, float value) {
		if (edge0 >= edge1)
			return value < edge0 ? 0.0F : 1.0F;
		float progress = Mth.clamp(
				(value - edge0) / (edge1 - edge0),
				0.0F,
				1.0F
		);
		return progress * progress * (3.0F - 2.0F * progress);
	}
}
