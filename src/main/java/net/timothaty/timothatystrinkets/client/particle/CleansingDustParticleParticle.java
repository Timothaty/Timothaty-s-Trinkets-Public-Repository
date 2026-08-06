package net.timothaty.timothatystrinkets.client.particle;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.multiplayer.ClientLevel;

@OnlyIn(Dist.CLIENT)
public class CleansingDustParticleParticle extends TextureSheetParticle {
	private final SpriteSet sprites;
	private final float baseSize;

	public static CleansingDustParticleParticleProvider provider(SpriteSet spriteSet) {
		return new CleansingDustParticleParticleProvider(spriteSet);
	}

	protected CleansingDustParticleParticle(ClientLevel level, double x, double y, double z,
										   double xd, double yd, double zd, SpriteSet spriteSet) {
		super(level, x, y, z);

		this.sprites = spriteSet;

		this.baseSize = 0.06F + this.random.nextFloat() * 0.06F;
		this.quadSize = this.baseSize;

		this.hasPhysics = true;
		this.gravity = 0.01F;
		this.friction = 0.92F;

		this.lifetime = 29 + this.random.nextInt(18);

		this.alpha = 0.85F;

		this.setColor(0.90F, 0.80F, 1.00F);

		this.xd = xd * 0.35D + this.random.nextGaussian() * 0.01D;
		this.yd = yd * 0.25D + this.random.nextGaussian() * 0.008D;
		this.zd = zd * 0.35D + this.random.nextGaussian() * 0.01D;

		this.roll = this.random.nextFloat() * ((float)Math.PI * 2F);
		this.oRoll = this.roll;

		this.pickSprite(spriteSet);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		this.xd += (this.random.nextFloat() - 0.5F) * 0.0022F;
		this.zd += (this.random.nextFloat() - 0.5F) * 0.0022F;

		if (this.random.nextFloat() < 0.20F) {
			this.yd += (this.random.nextFloat() - 0.5F) * 0.0012F;
		}

		super.tick();

		float t = (float)this.age / (float)this.lifetime;
		float fade = 1.0F - t;
		this.alpha = 0.85F * (fade * fade);

		if (this.onGround) {
			this.xd *= 0.65D;
			this.zd *= 0.65D;
		}

		this.setSpriteFromAge(this.sprites);
	}

	@Override
	public float getQuadSize(float partialTick) {
		float t = ((float)this.age + partialTick) / (float)this.lifetime;
		float s = 1.0F - t;
		return this.baseSize * (0.70F + 0.30F * s);
	}

	public static class CleansingDustParticleParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public CleansingDustParticleParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
									   double x, double y, double z,
									   double xd, double yd, double zd) {
			return new CleansingDustParticleParticle(level, x, y, z, xd, yd, zd, this.spriteSet);
		}
	}
}
