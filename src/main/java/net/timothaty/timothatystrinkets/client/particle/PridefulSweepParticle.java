package net.timothaty.timothatystrinkets.client.particle;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

@OnlyIn(Dist.CLIENT)
public class PridefulSweepParticle extends FierySweepParticleParticle {
	public static PridefulSweepParticleProvider provider(SpriteSet spriteSet) {
		return new PridefulSweepParticleProvider(spriteSet);
	}

	public static class PridefulSweepParticleProvider extends FierySweepParticleParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public PridefulSweepParticleProvider(SpriteSet spriteSet) {
			super(spriteSet);
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new PridefulSweepParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

	protected PridefulSweepParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(world, x, y, z, vx, vy, vz, spriteSet);
	}
}
