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
public class CleansingDustParticleUpParticle extends TextureSheetParticle {
	private final SpriteSet spriteSet;
	private final float baseSize;

	public static CleansingDustParticleUpParticleProvider provider(SpriteSet spriteSet) {
		return new CleansingDustParticleUpParticleProvider(spriteSet);
	}

	protected CleansingDustParticleUpParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(level, x, y, z);
		this.spriteSet = spriteSet;
		this.baseSize = 0.035F + this.random.nextFloat() * 0.035F;
		this.quadSize = this.baseSize;
		this.setSize(0.03F, 0.03F);
		this.lifetime = 60 + this.random.nextInt(41);
		this.gravity = 0.0F;
		this.friction = 0.985F;
		this.hasPhysics = false;
		this.xd = vx * 0.15D + (this.random.nextDouble() - 0.5D) * 0.010D;
		this.yd = 0.010D + this.random.nextDouble() * 0.012D + vy * 0.10D;
		this.zd = vz * 0.15D + (this.random.nextDouble() - 0.5D) * 0.010D;
		this.alpha = 0.0F;
		this.setColor(1.0F, 0.56F + this.random.nextFloat() * 0.18F, 0.92F);
		this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);
		this.oRoll = this.roll;
		this.pickSprite(spriteSet);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		this.oRoll = this.roll;
		this.roll += 0.018F + this.random.nextFloat() * 0.012F;
		this.xd += Math.sin((this.age + this.random.nextFloat()) * 0.18D) * 0.00045D;
		this.zd += Math.cos((this.age + this.random.nextFloat()) * 0.16D) * 0.00045D;
		this.yd = Math.min(0.034D, this.yd + 0.00012D);

		super.tick();
		if (this.removed) {
			return;
		}

		this.setSpriteFromAge(this.spriteSet);
		float progress = Mth.clamp((float) this.age / (float) this.lifetime, 0.0F, 1.0F);
		float fadeIn = smoothstep(0.0F, 0.18F, progress);
		float fadeOut = 1.0F - smoothstep(0.68F, 1.0F, progress);
		this.alpha = 0.72F * fadeIn * fadeOut;
		this.quadSize = this.baseSize * (0.72F + 0.35F * fadeIn);
	}

	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	private static float smoothstep(float edge0, float edge1, float value) {
		if (edge0 == edge1) {
			return value < edge0 ? 0.0F : 1.0F;
		}

		float x = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}

	public static class CleansingDustParticleUpParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public CleansingDustParticleUpParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new CleansingDustParticleUpParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}
}
