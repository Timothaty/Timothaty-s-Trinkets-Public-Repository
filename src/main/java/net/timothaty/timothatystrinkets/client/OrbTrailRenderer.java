package net.timothaty.timothatystrinkets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.List;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

final class OrbTrailRenderer {
	static final float OUTER_HALF_WIDTH = 0.09F;
	static final float OUTER_MAX_ALPHA = 0.14F;
	static final float INNER_HALF_WIDTH = 0.035F;
	static final float INNER_MAX_ALPHA = 0.42F;
	static final float REDUCED_HALF_WIDTH = 0.055F;
	static final float REDUCED_MAX_ALPHA = 0.28F;

	private static final double MIN_SEGMENT_LENGTH_SQR = 1.0E-8D;
	private static final int MAX_RENDER_POINTS = 13;
	private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

	private OrbTrailRenderer() {
	}

	static void render(
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			List<OrbitingOrbCurioRenderer.OrbTrailPoint> history,
			double headX,
			double headY,
			double headZ,
			Vec3 localCameraPosition,
			int rgb,
			float intensity,
			Lod lod,
			float distanceFade
	) {
		float alphaMultiplier = clamp01(intensity) * clamp01(distanceFade);
		if (alphaMultiplier <= 0.001F || history.isEmpty()) {
			return;
		}

		Scratch scratch = SCRATCH.get();
		int pointCount = collectPoints(scratch, history, headX, headY, headZ, lod);
		if (pointCount < 2) {
			return;
		}

		calculateSides(scratch, pointCount, localCameraPosition.x, localCameraPosition.y, localCameraPosition.z);

		VertexConsumer consumer = bufferSource.getBuffer(TimothatysTrinketsRenderTypes.orbitingOrbTrail());
		Matrix4f pose = poseStack.last().pose();
		int red = rgb >> 16 & 0xFF;
		int green = rgb >> 8 & 0xFF;
		int blue = rgb & 0xFF;

		if (lod == Lod.FULL) {
			drawLayer(pose, consumer, scratch, pointCount, red, green, blue, OUTER_HALF_WIDTH, OUTER_MAX_ALPHA * alphaMultiplier);
			drawLayer(pose, consumer, scratch, pointCount, red, green, blue, INNER_HALF_WIDTH, INNER_MAX_ALPHA * alphaMultiplier);
		} else {
			drawLayer(pose, consumer, scratch, pointCount, red, green, blue, REDUCED_HALF_WIDTH, REDUCED_MAX_ALPHA * alphaMultiplier);
		}
	}

	private static int collectPoints(
			Scratch scratch,
			List<OrbitingOrbCurioRenderer.OrbTrailPoint> history,
			double headX,
			double headY,
			double headZ,
			Lod lod
	) {
		int pointCount = 0;
		int step = lod == Lod.FULL ? 1 : 2;
		for (int index = 0; index < history.size(); index += step) {
			OrbitingOrbCurioRenderer.OrbTrailPoint point = history.get(index);
			pointCount = appendPoint(scratch, pointCount, point.x(), point.y(), point.z());
		}

		return appendPoint(scratch, pointCount, headX, headY, headZ);
	}

	private static int appendPoint(Scratch scratch, int pointCount, double x, double y, double z) {
		if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
			return pointCount;
		}

		if (pointCount > 0) {
			double deltaX = x - scratch.x[pointCount - 1];
			double deltaY = y - scratch.y[pointCount - 1];
			double deltaZ = z - scratch.z[pointCount - 1];
			if (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ < MIN_SEGMENT_LENGTH_SQR) {
				scratch.x[pointCount - 1] = x;
				scratch.y[pointCount - 1] = y;
				scratch.z[pointCount - 1] = z;
				return pointCount;
			}
		}

		if (pointCount >= MAX_RENDER_POINTS) {
			return pointCount;
		}

		scratch.x[pointCount] = x;
		scratch.y[pointCount] = y;
		scratch.z[pointCount] = z;
		return pointCount + 1;
	}

	private static void calculateSides(Scratch scratch, int pointCount, double cameraX, double cameraY, double cameraZ) {
		for (int index = 0; index < pointCount; index++) {
			double tangentX;
			double tangentY;
			double tangentZ;
			if (index == 0) {
				tangentX = scratch.x[1] - scratch.x[0];
				tangentY = scratch.y[1] - scratch.y[0];
				tangentZ = scratch.z[1] - scratch.z[0];
			} else if (index == pointCount - 1) {
				tangentX = scratch.x[index] - scratch.x[index - 1];
				tangentY = scratch.y[index] - scratch.y[index - 1];
				tangentZ = scratch.z[index] - scratch.z[index - 1];
			} else {
				tangentX = scratch.x[index + 1] - scratch.x[index - 1];
				tangentY = scratch.y[index + 1] - scratch.y[index - 1];
				tangentZ = scratch.z[index + 1] - scratch.z[index - 1];
			}

			double tangentLengthSqr = tangentX * tangentX + tangentY * tangentY + tangentZ * tangentZ;
			if (!Double.isFinite(tangentLengthSqr) || tangentLengthSqr < MIN_SEGMENT_LENGTH_SQR) {
				int otherIndex = index < pointCount - 1 ? index + 1 : index - 1;
				tangentX = scratch.x[otherIndex] - scratch.x[index];
				tangentY = scratch.y[otherIndex] - scratch.y[index];
				tangentZ = scratch.z[otherIndex] - scratch.z[index];
			}

			double directionX = cameraX - scratch.x[index];
			double directionY = cameraY - scratch.y[index];
			double directionZ = cameraZ - scratch.z[index];
			double sideX = tangentY * directionZ - tangentZ * directionY;
			double sideY = tangentZ * directionX - tangentX * directionZ;
			double sideZ = tangentX * directionY - tangentY * directionX;
			double sideLengthSqr = sideX * sideX + sideY * sideY + sideZ * sideZ;

			if (!Double.isFinite(sideLengthSqr) || sideLengthSqr < MIN_SEGMENT_LENGTH_SQR) {
				if (tangentX * tangentX + tangentZ * tangentZ >= MIN_SEGMENT_LENGTH_SQR) {
					sideX = -tangentZ;
					sideY = 0.0D;
					sideZ = tangentX;
				} else {
					sideX = 0.0D;
					sideY = tangentZ;
					sideZ = -tangentY;
				}
				sideLengthSqr = sideX * sideX + sideY * sideY + sideZ * sideZ;
			}

			if (!Double.isFinite(sideLengthSqr) || sideLengthSqr < MIN_SEGMENT_LENGTH_SQR) {
				sideX = 1.0D;
				sideY = 0.0D;
				sideZ = 0.0D;
			} else {
				double inverseSideLength = 1.0D / Math.sqrt(sideLengthSqr);
				sideX *= inverseSideLength;
				sideY *= inverseSideLength;
				sideZ *= inverseSideLength;
			}

			if (index > 0 && sideX * scratch.sideX[index - 1] + sideY * scratch.sideY[index - 1] + sideZ * scratch.sideZ[index - 1] < 0.0D) {
				sideX = -sideX;
				sideY = -sideY;
				sideZ = -sideZ;
			}

			scratch.sideX[index] = sideX;
			scratch.sideY[index] = sideY;
			scratch.sideZ[index] = sideZ;
		}
	}

	private static void drawLayer(
			Matrix4f pose,
			VertexConsumer consumer,
			Scratch scratch,
			int pointCount,
			int red,
			int green,
			int blue,
			float halfWidth,
			float maxAlpha
	) {
		float progressStep = 1.0F / (pointCount - 1);
		for (int index = 0; index < pointCount - 1; index++) {
			double segmentX = scratch.x[index + 1] - scratch.x[index];
			double segmentY = scratch.y[index + 1] - scratch.y[index];
			double segmentZ = scratch.z[index + 1] - scratch.z[index];
			if (segmentX * segmentX + segmentY * segmentY + segmentZ * segmentZ < MIN_SEGMENT_LENGTH_SQR) {
				continue;
			}

			float startEase = smoothstep(index * progressStep);
			float endEase = smoothstep((index + 1) * progressStep);
			emitQuad(
					pose,
					consumer,
					scratch,
					index,
					halfWidth * startEase,
					halfWidth * endEase,
					red,
					green,
					blue,
					alphaByte(maxAlpha * startEase),
					alphaByte(maxAlpha * endEase)
			);
		}
	}

	private static void emitQuad(
			Matrix4f pose,
			VertexConsumer consumer,
			Scratch scratch,
			int index,
			float startWidth,
			float endWidth,
			int red,
			int green,
			int blue,
			int startAlpha,
			int endAlpha
	) {
		double startOffsetX = scratch.sideX[index] * startWidth;
		double startOffsetY = scratch.sideY[index] * startWidth;
		double startOffsetZ = scratch.sideZ[index] * startWidth;
		double endOffsetX = scratch.sideX[index + 1] * endWidth;
		double endOffsetY = scratch.sideY[index + 1] * endWidth;
		double endOffsetZ = scratch.sideZ[index + 1] * endWidth;

		vertex(pose, consumer, scratch.x[index] + startOffsetX, scratch.y[index] + startOffsetY, scratch.z[index] + startOffsetZ, red, green, blue, startAlpha);
		vertex(pose, consumer, scratch.x[index + 1] + endOffsetX, scratch.y[index + 1] + endOffsetY, scratch.z[index + 1] + endOffsetZ, red, green, blue, endAlpha);
		vertex(pose, consumer, scratch.x[index + 1] - endOffsetX, scratch.y[index + 1] - endOffsetY, scratch.z[index + 1] - endOffsetZ, red, green, blue, endAlpha);
		vertex(pose, consumer, scratch.x[index] - startOffsetX, scratch.y[index] - startOffsetY, scratch.z[index] - startOffsetZ, red, green, blue, startAlpha);
	}

	private static void vertex(Matrix4f pose, VertexConsumer consumer, double x, double y, double z, int red, int green, int blue, int alpha) {
		consumer.addVertex(pose, (float) x, (float) y, (float) z).setColor(red, green, blue, alpha);
	}

	private static float smoothstep(float value) {
		float clamped = clamp01(value);
		return clamped * clamped * (3.0F - 2.0F * clamped);
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private static int alphaByte(float alpha) {
		return Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
	}

	enum Lod {
		FULL,
		REDUCED
	}

	private static final class Scratch {
		private final double[] x = new double[MAX_RENDER_POINTS];
		private final double[] y = new double[MAX_RENDER_POINTS];
		private final double[] z = new double[MAX_RENDER_POINTS];
		private final double[] sideX = new double[MAX_RENDER_POINTS];
		private final double[] sideY = new double[MAX_RENDER_POINTS];
		private final double[] sideZ = new double[MAX_RENDER_POINTS];
	}
}
