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

@OnlyIn(Dist.CLIENT)
public class FierySweepParticleParticle extends TextureSheetParticle {
	private static final int FRAME_COUNT = 8;
	private static final int LIFETIME_TICKS = 8;
	private static final float BASE_QUAD_SIZE = 1F;

	private final SpriteSet spriteSet;

	public static FierySweepParticleParticleProvider provider(SpriteSet spriteSet) {
		return new FierySweepParticleParticleProvider(spriteSet);
	}

	public static class FierySweepParticleParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public FierySweepParticleParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new FierySweepParticleParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

	protected FierySweepParticleParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(world, x, y, z);
		this.spriteSet = spriteSet;
		this.setSize(0.01F, 0.01F);
		this.lifetime = LIFETIME_TICKS;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.xd = 0.0D;
		this.yd = 0.0D;
		this.zd = 0.0D;
		this.quadSize = BASE_QUAD_SIZE;
		this.setAlpha(1.0F);
		this.setSpriteForAge();
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

		float progress = Math.min(1.0F, this.age / (float) this.lifetime);
		this.quadSize = BASE_QUAD_SIZE * (1.0F + 0.12F * progress);
		this.setAlpha(1.0F - 0.35F * progress);
		this.setSpriteForAge();
	}

	private void setSpriteForAge() {
		int frame = Math.min(FRAME_COUNT - 1, Math.max(0, this.age));
		this.setSprite(this.spriteSet.get(frame, FRAME_COUNT - 1));
	}
}