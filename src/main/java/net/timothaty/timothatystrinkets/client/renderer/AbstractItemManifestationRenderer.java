package net.timothaty.timothatystrinkets.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.timothaty.timothatystrinkets.entity.AbstractItemManifestationEntity;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractItemManifestationRenderer<T extends AbstractItemManifestationEntity> extends EntityRenderer<T> {
	private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0D) / 2.0D);
	private static final float DIRECTION_EPSILON = 1.0E-4F;
	private static final float MAX_ANGULAR_SWAY_VELOCITY_RATIO = 0.80F;
	private static final int MAX_RAY_COUNT = 64;
	private static final int HASH_CHANNEL_STRIDE = 16;
	private static final long HASH_INCREMENT = 0x9E3779B97F4A7C15L;
	private static final float UNIT_FLOAT_SCALE = 0x1.0p-24F;

	private final ItemRenderer itemRenderer;
	private final Vector3f rayOrigin = new Vector3f();
	private final Vector3f rayEndFirst = new Vector3f();
	private final Vector3f rayEndSecond = new Vector3f();
	private final Vector3f rayEndThird = new Vector3f();

	protected AbstractItemManifestationRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
		this.shadowRadius = 0.0F;
		this.shadowStrength = 0.0F;
	}

	protected abstract ManifestationRenderStyle style();

	@Override
	public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
		ItemStack stack = entity.getDisplayStack();
		if (stack.isEmpty()) return;

		ManifestationRenderStyle style = style();
		float age = entity.getManifestationAge() + partialTick;
		float remaining = entity.getConfiguredLifetime() - age;
		float itemVisibility = reveal(age, style.itemRevealTicks()) * reveal(remaining, style.itemFadeTicks());
		float rayVisibility = reveal(age, style.rayRevealTicks()) * reveal(remaining, style.rayFadeTicks());
		float bob = Mth.sin(age * style.itemBobSpeed()) * style.itemBobAmplitude();

		poseStack.pushPose();
		poseStack.translate(0.0F, bob, 0.0F);
		poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

		if (style.rayCount() > 0 && rayVisibility > 0.001F) {
			poseStack.pushPose();
			// Camera.rotation maps local -Z to the camera look vector, so negative Z is farther from the camera.
			poseStack.translate(0.0F, 0.0F, -Math.max(0.0F, style.rayBackOffset()));
			poseStack.mulPose(Axis.ZP.rotation(age * style.rayRotationSpeed()));
			if (style.rayGeometry() == ManifestationRayGeometry.FLAT_WEDGE) {
				renderFlatWedgeRays(entity, age, rayVisibility, style, poseStack,
						buffers.getBuffer(ManifestationRenderTypes.flatWedge()));
			} else {
				renderTriangularVolumeRays(entity, age, rayVisibility, style, poseStack,
						buffers.getBuffer(RenderType.dragonRays()));
				renderTriangularVolumeRays(entity, age, rayVisibility, style, poseStack,
						buffers.getBuffer(RenderType.dragonRaysDepth()));
			}
			poseStack.popPose();
		}

		if (itemVisibility > 0.001F) {
			poseStack.pushPose();
			poseStack.translate(0.0F, style.itemRenderOffsetY(), 0.0F);
			float scale = style.itemScale() * itemVisibility;
			poseStack.scale(scale, scale, scale);
			this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
					OverlayTexture.NO_OVERLAY, poseStack, buffers, entity.level(), entity.getId());
			poseStack.popPose();
		}

		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	private void renderTriangularVolumeRays(T entity, float age, float visibility, ManifestationRenderStyle style,
			PoseStack poseStack, VertexConsumer consumer) {
		int count = Mth.clamp(style.rayCount(), 0, MAX_RAY_COUNT);
		if (count == 0) return;

		long seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
		PoseStack.Pose pose = poseStack.last();

		for (int i = 0; i < count; i++) {
			float angle = deterministicFloat(seed, i, 0) * Mth.TWO_PI;
			float radialX = Mth.cos(angle);
			float radialY = Mth.sin(angle);
			float backwardTilt = Math.max(0.0F, Mth.lerp(deterministicFloat(seed, i, 1),
					style.rayMinBackwardTilt(), style.rayMaxBackwardTilt()));
			float directionScale = 1.0F / Mth.sqrt(1.0F + backwardTilt * backwardTilt);
			float directionX = radialX * directionScale;
			float directionY = radialY * directionScale;
			float directionZ = -backwardTilt * directionScale;

			float lengthFactor = (float)Math.pow(deterministicFloat(seed, i, 2),
					Math.max(0.01F, style.rayLengthDistributionPower()));
			float baseLength = Mth.lerp(lengthFactor, style.rayMinLength(), style.rayMaxLength());
			float baseWidth = Mth.lerp(deterministicFloat(seed, i, 3), style.rayMinWidth(), style.rayMaxWidth());
			float roll = deterministicFloat(seed, i, 4) * Mth.TWO_PI;
			float lengthPhase = deterministicFloat(seed, i, 5) * Mth.TWO_PI;
			float widthPhase = deterministicFloat(seed, i, 6) * Mth.TWO_PI;
			float pulseSpeed = Mth.lerp(deterministicFloat(seed, i, 7),
					style.rayMinPulseSpeed(), style.rayMaxPulseSpeed());
			float alphaMultiplier = Mth.lerp(deterministicFloat(seed, i, 8),
					style.rayMinAlphaMultiplier(), style.rayMaxAlphaMultiplier());
			float baseOffset = Mth.lerp(deterministicFloat(seed, i, 9),
					style.rayMinBaseOffset(), style.rayMaxBaseOffset()) * visibility;
			float extraDepth = Math.max(0.0F, style.rayDepthSpread()) * deterministicFloat(seed, i, 10);

			float lengthPulse = Mth.sin(age * pulseSpeed + lengthPhase);
			float widthPulse = Mth.sin(age * pulseSpeed + widthPhase);
			float length = Math.max(0.0F, baseLength * (1.0F + lengthPulse * style.rayLengthPulseAmount())) * visibility;
			float width = Math.max(0.0F, baseWidth * (1.0F + widthPulse * style.rayWidthPulseAmount())) * visibility;

			float basisUX = -radialY;
			float basisUY = radialX;
			float basisVX = -directionZ * radialX;
			float basisVY = -directionZ * radialY;
			float basisVZ = directionScale;
			float rollCos = Mth.cos(roll);
			float rollSin = Mth.sin(roll);
			float rolledUX = basisUX * rollCos + basisVX * rollSin;
			float rolledUY = basisUY * rollCos + basisVY * rollSin;
			float rolledUZ = basisVZ * rollSin;
			float rolledVX = -basisUX * rollSin + basisVX * rollCos;
			float rolledVY = -basisUY * rollSin + basisVY * rollCos;
			float rolledVZ = basisVZ * rollCos;

			float originX = radialX * baseOffset;
			float originY = radialY * baseOffset;
			float endX = originX + directionX * length;
			float endY = originY + directionY * length;
			float endZ = directionZ * length;
			this.rayEndFirst.set(
					endX + rolledUX * width,
					endY + rolledUY * width,
					endZ + rolledUZ * width);
			this.rayEndSecond.set(
					endX + (-0.5F * rolledUX + HALF_SQRT_3 * rolledVX) * width,
					endY + (-0.5F * rolledUY + HALF_SQRT_3 * rolledVY) * width,
					endZ + (-0.5F * rolledUZ + HALF_SQRT_3 * rolledVZ) * width);
			this.rayEndThird.set(
					endX + (-0.5F * rolledUX - HALF_SQRT_3 * rolledVX) * width,
					endY + (-0.5F * rolledUY - HALF_SQRT_3 * rolledVY) * width,
					endZ + (-0.5F * rolledUZ - HALF_SQRT_3 * rolledVZ) * width);

			float nearestVertexZ = Math.max(0.0F,
					Math.max(this.rayEndFirst.z(), Math.max(this.rayEndSecond.z(), this.rayEndThird.z())));
			float depthShift = -Math.max(0.0F, style.rayPlaneGap()) - extraDepth - nearestVertexZ;
			this.rayOrigin.set(originX, originY, depthShift);
			this.rayEndFirst.add(0.0F, 0.0F, depthShift);
			this.rayEndSecond.add(0.0F, 0.0F, depthShift);
			this.rayEndThird.add(0.0F, 0.0F, depthShift);

			int baseAlpha = Mth.clamp(Mth.floor(style.rayBaseAlpha() * visibility * alphaMultiplier), 0, 255);
			int tipAlpha = Mth.clamp(Mth.floor(style.rayTipAlpha() * visibility * alphaMultiplier), 0, 255);
			int baseColor = withAlpha(style.rayPrimaryColor(), baseAlpha);
			int tipColor = withAlpha(style.raySecondaryColor(), tipAlpha);
			triangle(consumer, pose, this.rayOrigin, this.rayEndFirst, this.rayEndSecond, baseColor, tipColor);
			triangle(consumer, pose, this.rayOrigin, this.rayEndSecond, this.rayEndThird, baseColor, tipColor);
			triangle(consumer, pose, this.rayOrigin, this.rayEndThird, this.rayEndFirst, baseColor, tipColor);
		}
	}

	private void renderFlatWedgeRays(T entity, float age, float visibility, ManifestationRenderStyle style,
			PoseStack poseStack, VertexConsumer consumer) {
		int count = Mth.clamp(style.rayCount(), 0, MAX_RAY_COUNT);
		if (count == 0) return;

		long seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
		PoseStack.Pose pose = poseStack.last();
		float sectorSize = Mth.TWO_PI / count;
		float angularJitter = Math.max(0.0F, style.rayAngularJitter());
		float verticalScale = Math.max(0.0F, style.rayVerticalScale());
		float minAngularSwaySectorFraction = Math.max(0.0F,
				Math.min(style.rayMinAngularSwaySectorFraction(), style.rayMaxAngularSwaySectorFraction()));
		float maxAngularSwaySectorFraction = Math.max(minAngularSwaySectorFraction,
				Math.max(style.rayMinAngularSwaySectorFraction(), style.rayMaxAngularSwaySectorFraction()));
		float minAngularSwaySpeed = Math.max(0.0F,
				Math.min(style.rayMinAngularSwaySpeed(), style.rayMaxAngularSwaySpeed()));
		float maxAngularSwaySpeed = Math.max(minAngularSwaySpeed,
				Math.max(style.rayMinAngularSwaySpeed(), style.rayMaxAngularSwaySpeed()));
		float minLocalRollSpeed = Math.max(0.0F,
				Math.min(style.rayMinLocalRollSpeed(), style.rayMaxLocalRollSpeed()));
		float maxLocalRollSpeed = Math.max(minLocalRollSpeed,
				Math.max(style.rayMinLocalRollSpeed(), style.rayMaxLocalRollSpeed()));
		boolean localRollEnabled = maxLocalRollSpeed > 0.0F;

		for (int i = 0; i < count; i++) {
			float baseAngle = i * sectorSize;
			float jitter = (deterministicFloat(seed, i, 0) - 0.5F) * sectorSize * angularJitter;
			float angularSwayAmplitude = sectorSize * Mth.lerp(deterministicFloat(seed, i, 12),
					minAngularSwaySectorFraction, maxAngularSwaySectorFraction);
			float requestedAngularSwaySpeed = Mth.lerp(deterministicFloat(seed, i, 13),
					minAngularSwaySpeed, maxAngularSwaySpeed);
			float angularSwaySpeed = clampAngularSwaySpeed(requestedAngularSwaySpeed,
					angularSwayAmplitude, style.rayRotationSpeed());
			float angularSwayPhase = deterministicFloat(seed, i, 14) * Mth.TWO_PI;
			float individualAngularOffset = angularSwaySpeed > 0.0F
					? Mth.sin(age * angularSwaySpeed + angularSwayPhase) * angularSwayAmplitude
					: 0.0F;
			float angle = baseAngle + jitter + individualAngularOffset;
			float directionX = Mth.cos(angle);
			float directionY = Mth.sin(angle) * verticalScale;
			float directionLength = Mth.sqrt(directionX * directionX + directionY * directionY);
			if (directionLength < DIRECTION_EPSILON) {
				directionX = directionX < 0.0F ? -1.0F : 1.0F;
				directionY = 0.0F;
			} else {
				directionX /= directionLength;
				directionY /= directionLength;
			}

			float lengthFactor = (float)Math.pow(deterministicFloat(seed, i, 2),
					Math.max(0.01F, style.rayLengthDistributionPower()));
			float baseLength = Mth.lerp(lengthFactor, style.rayMinLength(), style.rayMaxLength());
			float baseWidth = Mth.lerp(deterministicFloat(seed, i, 3), style.rayMinWidth(), style.rayMaxWidth());
			float pulsePhase = deterministicFloat(seed, i, 4) * Mth.TWO_PI;
			float pulseSpeed = Mth.lerp(deterministicFloat(seed, i, 5),
					style.rayMinPulseSpeed(), style.rayMaxPulseSpeed());
			float alphaMultiplier = Mth.lerp(deterministicFloat(seed, i, 6),
					style.rayMinAlphaMultiplier(), style.rayMaxAlphaMultiplier());
			float baseOffset = Mth.lerp(deterministicFloat(seed, i, 7),
					style.rayMinBaseOffset(), style.rayMaxBaseOffset()) * visibility;
			float extraDepth = Math.max(0.0F, style.rayDepthSpread()) * deterministicFloat(seed, i, 8);
			float localRoll = 0.0F;
			if (localRollEnabled) {
				float initialRoll = deterministicFloat(seed, i, 9) * Mth.TWO_PI;
				float rollDirection = deterministicFloat(seed, i, 10) < 0.5F ? -1.0F : 1.0F;
				float rollSpeed = Mth.lerp(deterministicFloat(seed, i, 11),
						minLocalRollSpeed, maxLocalRollSpeed) * rollDirection;
				localRoll = initialRoll + age * rollSpeed;
			}

			float pulse = Mth.sin(age * pulseSpeed + pulsePhase);
			float length = Math.max(0.0F, baseLength * (1.0F + pulse * style.rayLengthPulseAmount())) * visibility;
			float width = Math.max(0.0F, baseWidth * (1.0F + pulse * style.rayWidthPulseAmount())) * visibility;
			float sideX = -directionY;
			float sideY = directionX;
			float rollCos = Mth.cos(localRoll);
			float rollSin = Mth.sin(localRoll);
			// For U=(-Dy,Dx,0), D cross U is +Z; only the width axis rotates around the fixed D axis.
			float widthAxisX = sideX * rollCos;
			float widthAxisY = sideY * rollCos;
			float widthAxisZ = rollSin;
			float originX = directionX * baseOffset;
			float originY = directionY * baseOffset;
			float endX = originX + directionX * length;
			float endY = originY + directionY * length;
			float originZ = -Math.max(0.0F, style.rayPlaneGap()) - extraDepth;
			float leftX = endX + widthAxisX * width;
			float leftY = endY + widthAxisY * width;
			float leftZ = originZ + widthAxisZ * width;
			float rightX = endX - widthAxisX * width;
			float rightY = endY - widthAxisY * width;
			float rightZ = originZ - widthAxisZ * width;

			float maximumAllowedZ = -Math.max(0.0F, style.rayPlaneGap());
			float nearestZ = Math.max(originZ, Math.max(leftZ, rightZ));
			float depthCorrection = Math.max(0.0F, nearestZ - maximumAllowedZ);
			originZ -= depthCorrection;
			leftZ -= depthCorrection;
			rightZ -= depthCorrection;

			int baseAlpha = Mth.clamp(Mth.floor(style.rayBaseAlpha() * visibility * alphaMultiplier), 0, 255);
			int tipAlpha = Mth.clamp(Mth.floor(style.rayTipAlpha() * visibility * alphaMultiplier), 0, 255);
			int baseColor = withAlpha(style.rayPrimaryColor(), baseAlpha);
			int tipColor = withAlpha(style.raySecondaryColor(), tipAlpha);
			flatWedge(consumer, pose,
					originX, originY, originZ,
					leftX, leftY, leftZ,
					rightX, rightY, rightZ,
					baseColor, tipColor);
		}
	}

	private static void flatWedge(VertexConsumer consumer, PoseStack.Pose pose,
			float originX, float originY, float originZ,
			float leftX, float leftY, float leftZ,
			float rightX, float rightY, float rightZ,
			int originColor, int tipColor) {
		// A duplicated outer vertex closes the wedge as a degenerate quad without adding depth.
		consumer.addVertex(pose.pose(), originX, originY, originZ).setColor(originColor);
		consumer.addVertex(pose.pose(), leftX, leftY, leftZ).setColor(tipColor);
		consumer.addVertex(pose.pose(), rightX, rightY, rightZ).setColor(tipColor);
		consumer.addVertex(pose.pose(), leftX, leftY, leftZ).setColor(tipColor);
	}

	private static void triangle(VertexConsumer consumer, PoseStack.Pose pose, Vector3f origin,
			Vector3f first, Vector3f second, int originColor, int endColor) {
		consumer.addVertex(pose, origin).setColor(originColor);
		consumer.addVertex(pose, first).setColor(endColor);
		consumer.addVertex(pose, second).setColor(endColor);
	}

	@Override
	public boolean shouldRender(T entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
		ManifestationRenderStyle style = style();
		double itemExtent = Math.abs(style.itemScale()) + Math.abs(style.itemRenderOffsetY());
		double rayLength = Math.max(Math.abs(style.rayMinLength()), Math.abs(style.rayMaxLength()))
				* (1.0D + Math.abs(style.rayLengthPulseAmount()));
		double rayWidth = Math.max(Math.abs(style.rayMinWidth()), Math.abs(style.rayMaxWidth()))
				* (1.0D + Math.abs(style.rayWidthPulseAmount()));
		double baseOffset = Math.max(Math.abs(style.rayMinBaseOffset()), Math.abs(style.rayMaxBaseOffset()));
		double localRollDepth = style.rayGeometry() == ManifestationRayGeometry.FLAT_WEDGE
				&& Math.max(style.rayMinLocalRollSpeed(), style.rayMaxLocalRollSpeed()) > 0.0F
				? rayWidth
				: 0.0D;
		double rayExtent = baseOffset + rayLength + rayWidth + Math.abs(style.rayBackOffset())
				+ Math.abs(style.rayPlaneGap()) + Math.abs(style.rayDepthSpread()) + localRollDepth;
		double expansion = Math.max(itemExtent, rayExtent) + Math.abs(style.itemBobAmplitude());
		return entity.shouldRender(cameraX, cameraY, cameraZ)
				&& frustum.isVisible(entity.getBoundingBoxForCulling().inflate(expansion));
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return InventoryMenu.BLOCK_ATLAS;
	}

	private static float reveal(float age, int ticks) {
		float value = Mth.clamp(age / Math.max(1.0F, ticks), 0.0F, 1.0F);
		return value * value * (3.0F - 2.0F * value);
	}

	private static float clampAngularSwaySpeed(float requestedSpeed, float amplitude, float globalRotationSpeed) {
		float maxIndividualAngularVelocity = Math.abs(globalRotationSpeed) * MAX_ANGULAR_SWAY_VELOCITY_RATIO;
		if (requestedSpeed <= 0.0F || amplitude <= DIRECTION_EPSILON || maxIndividualAngularVelocity <= 0.0F) {
			return 0.0F;
		}
		return Math.min(requestedSpeed, maxIndividualAngularVelocity / amplitude);
	}

	private static int withAlpha(int rgb, int alpha) {
		return alpha << 24 | rgb & 0xFFFFFF;
	}

	private static float deterministicFloat(long seed, int rayIndex, int channel) {
		long value = seed + HASH_INCREMENT * (long)(rayIndex * HASH_CHANNEL_STRIDE + channel + 1);
		value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
		value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
		value ^= value >>> 31;
		return (float)(value >>> 40) * UNIT_FLOAT_SCALE;
	}

	protected record ManifestationRenderStyle(
			float itemScale,
			float itemBobAmplitude,
			float itemBobSpeed,
			float itemRenderOffsetY,
			int itemRevealTicks,
			int itemFadeTicks,
			int rayPrimaryColor,
			int raySecondaryColor,
			int rayCount,
			ManifestationRayGeometry rayGeometry,
			float rayAngularJitter,
			float rayMinAngularSwaySectorFraction,
			float rayMaxAngularSwaySectorFraction,
			float rayMinAngularSwaySpeed,
			float rayMaxAngularSwaySpeed,
			float rayBackOffset,
			float rayPlaneGap,
			float rayDepthSpread,
			float rayVerticalScale,
			float rayMinBaseOffset,
			float rayMaxBaseOffset,
			float rayMinBackwardTilt,
			float rayMaxBackwardTilt,
			float rayMinLength,
			float rayMaxLength,
			float rayLengthDistributionPower,
			float rayMinWidth,
			float rayMaxWidth,
			float rayRotationSpeed,
			float rayMinLocalRollSpeed,
			float rayMaxLocalRollSpeed,
			float rayMinPulseSpeed,
			float rayMaxPulseSpeed,
			float rayLengthPulseAmount,
			float rayWidthPulseAmount,
			float rayMinAlphaMultiplier,
			float rayMaxAlphaMultiplier,
			int rayRevealTicks,
			int rayFadeTicks,
			int rayBaseAlpha,
			int rayTipAlpha
	) {
	}
}
