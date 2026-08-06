package net.timothaty.timothatystrinkets.client.particle;

import net.timothaty.timothatystrinkets.client.cherubims_wisdom.CherubimsWisdomActivationVisuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class CherubimsWisdomExperienceDotParticle extends TextureSheetParticle {
	private static final int LIFETIME_TICKS = 7;
	private static final Set<CherubimsWisdomExperienceDotParticle> ACTIVE_PARTICLES =
			Collections.newSetFromMap(new IdentityHashMap<>());

	private final SpriteSet spriteSet;
	private final int targetEntityId;
	private final Vec3 startPosition;
	private final Vec3 curvePerpendicular;
	private final double curveLift;
	private final float baseSize;
	private Vec3 lastKnownTarget;

	private CherubimsWisdomExperienceDotParticle(
			ClientLevel level,
			double x,
			double y,
			double z,
			double encodedTargetEntityId,
			double encodedCurveLift,
			double encodedCurveSide,
			SpriteSet spriteSet
	) {
		super(level, x, y, z);
		this.spriteSet = spriteSet;
		this.targetEntityId = Mth.floor(encodedTargetEntityId);
		this.startPosition = new Vec3(x, y, z);
		this.curveLift = Mth.clamp(encodedCurveLift, 0.05D, 0.75D);
		this.lastKnownTarget = resolveTargetChest();
		if (this.lastKnownTarget == null)
			this.lastKnownTarget = this.startPosition;

		Vec3 towardTarget = this.lastKnownTarget.subtract(this.startPosition);
		double horizontalLength = Math.sqrt(towardTarget.x * towardTarget.x + towardTarget.z * towardTarget.z);
		double curveSide = Mth.clamp(encodedCurveSide, -0.60D, 0.60D);
		this.curvePerpendicular = horizontalLength > 1.0E-5D
				? new Vec3(-towardTarget.z / horizontalLength * curveSide, 0.0D,
				towardTarget.x / horizontalLength * curveSide)
				: new Vec3(curveSide, 0.0D, 0.0D);

		this.lifetime = LIFETIME_TICKS;
		this.hasPhysics = false;
		this.gravity = 0.0F;
		this.alpha = 0.95F;
		this.baseSize = 0.065F + this.random.nextFloat() * 0.030F;
		this.quadSize = this.baseSize;
		this.setSize(0.02F, 0.02F);
		this.pickSprite(spriteSet);
		setPackedColor(CherubimsWisdomActivationVisuals.EXPERIENCE_GREEN_RGB);
		ACTIVE_PARTICLES.add(this);
	}

	public static ParticleProvider<SimpleParticleType> provider(SpriteSet spriteSet) {
		return (type, level, x, y, z, targetEntityId, curveLift, curveSide) ->
				new CherubimsWisdomExperienceDotParticle(
						level, x, y, z, targetEntityId, curveLift, curveSide, spriteSet);
	}

	public static void removeForTarget(int targetEntityId) {
		for (CherubimsWisdomExperienceDotParticle particle : List.copyOf(ACTIVE_PARTICLES)) {
			if (particle.targetEntityId == targetEntityId)
				particle.remove();
		}
	}

	public static void clearTrackedParticles() {
		for (CherubimsWisdomExperienceDotParticle particle : List.copyOf(ACTIVE_PARTICLES))
			particle.remove();
		ACTIVE_PARTICLES.clear();
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		if (Minecraft.getInstance().level != this.level) {
			remove();
			return;
		}

		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		if (this.age++ >= this.lifetime) {
			remove();
			return;
		}

		Vec3 currentTarget = resolveTargetChest();
		if (currentTarget != null)
			this.lastKnownTarget = currentTarget;

		float progress = Mth.clamp((float) this.age / (float) this.lifetime, 0.0F, 1.0F);
		Vec3 control = this.startPosition.lerp(this.lastKnownTarget, 0.5D)
				.add(this.curvePerpendicular.x, this.curveLift, this.curvePerpendicular.z);
		Vec3 position = quadraticBezier(this.startPosition, control, this.lastKnownTarget, progress);
		setPos(position.x, position.y, position.z);
		this.setSpriteFromAge(this.spriteSet);
		this.alpha = 0.95F * (1.0F - smoothstep(0.55F, 1.0F, progress));
		this.quadSize = this.baseSize * (1.0F - 0.65F * smoothstep(0.0F, 1.0F, progress));
	}

	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	@Override
	public void remove() {
		super.remove();
		ACTIVE_PARTICLES.remove(this);
	}

	private Vec3 resolveTargetChest() {
		Entity target = this.targetEntityId >= 0 ? this.level.getEntity(this.targetEntityId) : null;
		if (target == null || target.isRemoved() || !target.isAlive())
			return null;
		return new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.62D, target.getZ());
	}

	private void setPackedColor(int color) {
		setColor(
				((color >> 16) & 0xFF) / 255.0F,
				((color >> 8) & 0xFF) / 255.0F,
				(color & 0xFF) / 255.0F
		);
	}

	private static Vec3 quadraticBezier(Vec3 start, Vec3 control, Vec3 end, double progress) {
		double inverse = 1.0D - progress;
		return start.scale(inverse * inverse)
				.add(control.scale(2.0D * inverse * progress))
				.add(end.scale(progress * progress));
	}

	private static float smoothstep(float edge0, float edge1, float value) {
		value = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return value * value * (3.0F - 2.0F * value);
	}
}
