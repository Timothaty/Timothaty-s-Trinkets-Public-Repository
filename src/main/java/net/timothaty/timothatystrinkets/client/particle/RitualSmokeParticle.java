package net.timothaty.timothatystrinkets.client.particle;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.timothaty.timothatystrinkets.particle.RitualSmokeParticleOptions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class RitualSmokeParticle extends TextureSheetParticle {
	private final SpriteSet sprites;
	private final double targetX;
	private final double targetY;
	private final double targetZ;
	private final float baseSize;

	private RitualSmokeParticle(ClientLevel level, double x, double y, double z,
			RitualSmokeParticleOptions options, SpriteSet sprites) {
		super(level, x, y, z);
		this.sprites = sprites;
		Vector3f offset = options.targetOffset();
		this.targetX = x + offset.x();
		this.targetY = y + offset.y();
		this.targetZ = z + offset.z();
		this.baseSize = 0.22F + this.random.nextFloat() * 0.13F;
		this.quadSize = this.baseSize * 0.65F;
		this.lifetime = 34 + this.random.nextInt(18);
		this.hasPhysics = false;
		this.gravity = 0.0F;
		this.friction = 0.94F;
		this.alpha = 0.0F;
		this.xd = (this.random.nextDouble() - 0.5D) * 0.008D;
		this.yd = 0.008D + this.random.nextDouble() * 0.008D;
		this.zd = (this.random.nextDouble() - 0.5D) * 0.008D;
		this.pickSprite(sprites);
		this.setColor(0.72F, 0.66F, 0.69F);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		if (this.age++ >= this.lifetime) {
			this.remove();
			return;
		}

		double dx = this.targetX - this.x;
		double dz = this.targetZ - this.z;
		double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
		if (horizontalDistance > 1.0E-4D) {
			double nx = dx / horizontalDistance;
			double nz = dz / horizontalDistance;
			double inward = 0.0032D + Math.min(horizontalDistance, 3.0D) * 0.0008D;
			double tangent = 0.0026D;
			this.xd += nx * inward - nz * tangent;
			this.zd += nz * inward + nx * tangent;
		}
		this.yd += Mth.clamp((this.targetY - this.y) * 0.0009D, -0.001D, 0.0022D) + 0.0007D;
		this.move(this.xd, this.yd, this.zd);
		this.xd *= this.friction;
		this.yd *= 0.96D;
		this.zd *= this.friction;

		this.setSpriteFromAge(this.sprites);
		float progress = (float)this.age / (float)this.lifetime;
		float fadeIn = smoothstep(0.0F, 0.18F, progress);
		float fadeOut = 1.0F - smoothstep(0.72F, 1.0F, progress);
		this.alpha = 0.62F * fadeIn * fadeOut;
		this.quadSize = this.baseSize * (0.65F + progress * 0.85F);
	}

	private static float smoothstep(float edge0, float edge1, float value) {
		float x = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}

	public static ParticleProvider<RitualSmokeParticleOptions> provider(SpriteSet sprites) {
		return (options, level, x, y, z, xSpeed, ySpeed, zSpeed) -> new RitualSmokeParticle(level, x, y, z, options, sprites);
	}
}
