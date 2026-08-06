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
public class RottenChunkParticle extends TextureSheetParticle {
	private final float rotationSpeed;
	private boolean stuckToGround = false;

	public static RottenChunkParticleProvider provider(SpriteSet spriteSet) {
		return new RottenChunkParticleProvider(spriteSet);
	}

	public static class RottenChunkParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public RottenChunkParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new RottenChunkParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

	private final SpriteSet spriteSet;

	protected RottenChunkParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(world, x, y, z);
		this.spriteSet = spriteSet;
		this.setSize(0.2f, 0.2f);
		this.lifetime = 20 + this.random.nextInt(6);
		this.gravity = 1.45f;
		this.hasPhysics = true;
		this.friction = 0.9F;
		this.xd = vx;
		this.yd = vy;
		this.zd = vz;
		this.rotationSpeed = (this.random.nextFloat() - 0.5F) * 0.35F;
		this.roll = this.random.nextFloat() * ((float) Math.PI * 2F);
		this.oRoll = this.roll;
		this.pickSprite(spriteSet);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}

	@Override
	public void tick() {
		this.oRoll = this.roll;
		if (!this.stuckToGround) {
			this.roll += this.rotationSpeed;
		}

		super.tick();

		if (this.removed) {
			return;
		}

		if (this.onGround && !this.stuckToGround) {
			this.stuckToGround = true;
			this.xd *= 0.08D;
			this.yd = 0.0D;
			this.zd *= 0.08D;
			this.gravity = 0.0F;
		}
	}
}
