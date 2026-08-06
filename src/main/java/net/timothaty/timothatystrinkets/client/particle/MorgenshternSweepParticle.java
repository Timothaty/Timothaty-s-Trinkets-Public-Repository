package net.timothaty.timothatystrinkets.client.particle;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

@OnlyIn(Dist.CLIENT)
public final class MorgenshternSweepParticle extends TextureSheetParticle {
	private static final int LIFETIME_TICKS = 4;
	private static final float MINIMUM_SIZE = 0.68F;
	private static final float MAXIMUM_SIZE = 1.35F;

	private final SpriteSet sprites;

	private MorgenshternSweepParticle(
			ClientLevel level,
			double x,
			double y,
			double z,
			double size,
			SpriteSet sprites
	) {
		super(level, x, y, z, 0.0D, 0.0D, 0.0D);
		this.sprites = sprites;
		this.lifetime = LIFETIME_TICKS;
		this.quadSize = Mth.clamp(
				(float) size,
				MINIMUM_SIZE,
				MAXIMUM_SIZE
		);
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.xd = 0.0D;
		this.yd = 0.0D;
		this.zd = 0.0D;

		float shade = this.random.nextFloat() * 0.6F + 0.4F;
		this.rCol = shade;
		this.gCol = shade;
		this.bCol = shade;

		float sweepRoll = Mth.HALF_PI;
		this.roll = sweepRoll;
		this.oRoll = sweepRoll;
		this.setSpriteFromAge(this.sprites);
	}

	public static ParticleProvider<SimpleParticleType> provider(
			SpriteSet sprites
	) {
		return (
				type,
				level,
				x,
				y,
				z,
				size,
				ySpeed,
				zSpeed
		) -> new MorgenshternSweepParticle(
				level,
				x,
				y,
				z,
				size,
				sprites
		);
	}

	@Override
	public int getLightColor(float partialTick) {
		return 15728880;
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
		this.setSpriteFromAge(this.sprites);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_LIT;
	}
}
