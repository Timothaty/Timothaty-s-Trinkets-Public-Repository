package net.timothaty.timothatystrinkets.client.particle;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

@OnlyIn(Dist.CLIENT)
public class BloodBitParticle extends TextureSheetParticle {
	private final float baseSize;
	private boolean stuckToGround = false;

	public static BloodBitParticleProvider provider(SpriteSet spriteSet) {
		return new BloodBitParticleProvider(spriteSet);
	}

	protected BloodBitParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, xd, yd, zd);

		this.hasPhysics = true;
		this.friction = 0.86F;
		this.gravity = 0.90F;

		this.baseSize = 0.07F + this.random.nextFloat() * 0.05F;
		this.quadSize = this.baseSize;
		this.lifetime = 27 + this.random.nextInt(12);

		this.xd *= 0.35D;
		this.yd *= 0.35D;
		this.zd *= 0.35D;

		this.pickSprite(sprites);
		
		this.roll = this.random.nextFloat() * (float) (Math.PI * 2.0);
		this.oRoll = this.roll;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		this.oRoll = this.roll;

		if (!this.stuckToGround) {
			this.roll += 0.18F;
		}

		super.tick();

		if (this.removed) {
			return;
		}

		if (this.onGround && !this.stuckToGround) {
			this.stuckToGround = true;

			this.xd *= 0.05D;
			this.yd = 0.0D;
			this.zd *= 0.05D;

			this.gravity = 0.0F;
			this.friction = 0.92F;

			this.quadSize = this.baseSize * (1.25F + this.random.nextFloat() * 0.25F);
		}

		if (this.stuckToGround && this.age < 4) {
			this.quadSize = this.baseSize * (1.20F + this.age * 0.10F);
		}

		int fade = 7;
		if (this.age > this.lifetime - fade) {
			float t = (float) (this.lifetime - this.age) / (float) fade;
			this.alpha = 0.95F * Mth.clamp(t, 0.0F, 1.0F);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static class BloodBitParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public BloodBitParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType typeIn, ClientLevel level, double x, double y, double z,
				double xSpeed, double ySpeed, double zSpeed) {
			return new BloodBitParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}
}