package net.timothaty.timothatystrinkets.client.renderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.timothaty.timothatystrinkets.entity.CleansingDustManifestationEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

@OnlyIn(Dist.CLIENT)
public final class CleansingDustManifestationRenderer extends AbstractItemManifestationRenderer<CleansingDustManifestationEntity> {
	private static final int RAY_PRIMARY_COLOR = 0xEAFBFF;
	private static final int RAY_SECONDARY_COLOR = 0x57C7FF;
	private static final int RAY_COUNT = 14;
	private static final ManifestationRayGeometry RAY_GEOMETRY = ManifestationRayGeometry.FLAT_WEDGE;
	private static final float RAY_ANGULAR_JITTER = 0.8F;
	private static final float RAY_MIN_ANGULAR_SWAY_SECTOR_FRACTION = 0.22F;
	private static final float RAY_MAX_ANGULAR_SWAY_SECTOR_FRACTION = 0.48F;
	private static final float RAY_MIN_ANGULAR_SWAY_SPEED = 0.007F;
	private static final float RAY_MAX_ANGULAR_SWAY_SPEED = 0.012F;
	/** Depth gap that keeps the complete ray layer behind the item plane */
	private static final float RAY_BACK_OFFSET = 0.10F;
	/** Minimum local depth maintained by every vertex behind the ray layer origin */
	private static final float RAY_PLANE_GAP = 0.020F;
	/** Maximum additional depth into the back hemisphere */
	private static final float RAY_DEPTH_SPREAD = 0.0F;
	private static final float RAY_VERTICAL_SCALE = 0.82F;
	private static final float RAY_MIN_BASE_OFFSET = 0.0F;
	private static final float RAY_MAX_BASE_OFFSET = 0.025F;
	private static final float RAY_MIN_BACKWARD_TILT = 0.0F;
	private static final float RAY_MAX_BACKWARD_TILT = 0.0F;
	private static final float RAY_MIN_LENGTH = 0.52F;
	private static final float RAY_MAX_LENGTH = 0.90F;
	private static final float RAY_LENGTH_DISTRIBUTION_POWER = 1.15F;
	private static final float RAY_MIN_WIDTH = 0.050F;
	private static final float RAY_MAX_WIDTH = 0.085F;
	private static final float RAY_ROTATION_SPEED = 0.0035F;
	private static final float RAY_MIN_LOCAL_ROLL_SPEED = 0.006F;
	private static final float RAY_MAX_LOCAL_ROLL_SPEED = 0.012F;
	private static final float RAY_MIN_PULSE_SPEED = 0.095F;
	private static final float RAY_MAX_PULSE_SPEED = 0.105F;
	private static final float RAY_LENGTH_PULSE_AMOUNT = 0.15F;
	private static final float RAY_WIDTH_PULSE_AMOUNT = 0.10F;
	private static final float RAY_MIN_ALPHA_MULTIPLIER = 0.85F;
	private static final float RAY_MAX_ALPHA_MULTIPLIER = 1.0F;
	private static final int RAY_REVEAL_TICKS = 16;
	private static final int RAY_FADE_TICKS = 20;
	private static final int RAY_BASE_ALPHA = 225;
	private static final int RAY_TIP_ALPHA = 0;

	private static final float ITEM_SCALE = 0.72F;
	private static final float ITEM_BOB_AMPLITUDE = 0.10F;
	private static final float ITEM_BOB_SPEED = 0.12F;
	private static final float ITEM_RENDER_OFFSET_Y = 0.0F;
	private static final int ITEM_REVEAL_TICKS = 12;
	private static final int ITEM_FADE_TICKS = 20;

	private static final ManifestationRenderStyle STYLE = new ManifestationRenderStyle(
			ITEM_SCALE,
			ITEM_BOB_AMPLITUDE,
			ITEM_BOB_SPEED,
			ITEM_RENDER_OFFSET_Y,
			ITEM_REVEAL_TICKS,
			ITEM_FADE_TICKS,
			RAY_PRIMARY_COLOR,
			RAY_SECONDARY_COLOR,
			RAY_COUNT,
			RAY_GEOMETRY,
			RAY_ANGULAR_JITTER,
			RAY_MIN_ANGULAR_SWAY_SECTOR_FRACTION,
			RAY_MAX_ANGULAR_SWAY_SECTOR_FRACTION,
			RAY_MIN_ANGULAR_SWAY_SPEED,
			RAY_MAX_ANGULAR_SWAY_SPEED,
			RAY_BACK_OFFSET,
			RAY_PLANE_GAP,
			RAY_DEPTH_SPREAD,
			RAY_VERTICAL_SCALE,
			RAY_MIN_BASE_OFFSET,
			RAY_MAX_BASE_OFFSET,
			RAY_MIN_BACKWARD_TILT,
			RAY_MAX_BACKWARD_TILT,
			RAY_MIN_LENGTH,
			RAY_MAX_LENGTH,
			RAY_LENGTH_DISTRIBUTION_POWER,
			RAY_MIN_WIDTH,
			RAY_MAX_WIDTH,
			RAY_ROTATION_SPEED,
			RAY_MIN_LOCAL_ROLL_SPEED,
			RAY_MAX_LOCAL_ROLL_SPEED,
			RAY_MIN_PULSE_SPEED,
			RAY_MAX_PULSE_SPEED,
			RAY_LENGTH_PULSE_AMOUNT,
			RAY_WIDTH_PULSE_AMOUNT,
			RAY_MIN_ALPHA_MULTIPLIER,
			RAY_MAX_ALPHA_MULTIPLIER,
			RAY_REVEAL_TICKS,
			RAY_FADE_TICKS,
			RAY_BASE_ALPHA,
			RAY_TIP_ALPHA
	);

	public CleansingDustManifestationRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected ManifestationRenderStyle style() {
		return STYLE;
	}
}
