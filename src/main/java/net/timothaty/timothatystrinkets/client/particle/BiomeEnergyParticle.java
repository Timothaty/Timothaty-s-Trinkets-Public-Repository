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
import net.minecraft.util.Mth;

@OnlyIn(Dist.CLIENT)
public class BiomeEnergyParticle extends TextureSheetParticle {
	public static BiomeEnergyParticleProvider provider(SpriteSet spriteSet) {
		return new BiomeEnergyParticleProvider(spriteSet);
	}

	public static class BiomeEnergyParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public BiomeEnergyParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new BiomeEnergyParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

	private final SpriteSet spriteSet;
	private final float baseSize;
	private final float spinSpeed;
	private final double driftPhase;

	protected BiomeEnergyParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(world, x, y, z);
		this.spriteSet = spriteSet;
		this.baseSize = 0.052F + this.random.nextFloat() * 0.038F;
		this.quadSize = this.baseSize;
		this.setSize(0.02F, 0.02F);
		this.lifetime = 42 + this.random.nextInt(28);
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.friction = 0.975F;
		this.alpha = 0.0F;
		this.xd = (this.random.nextDouble() - 0.5D) * 0.0035D;
		this.yd = 0.0045D + this.random.nextDouble() * 0.0030D;
		this.zd = (this.random.nextDouble() - 0.5D) * 0.0035D;
		this.driftPhase = this.random.nextDouble() * Math.PI * 2.0D;
		this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);
		this.oRoll = this.roll;
		this.spinSpeed = (this.random.nextFloat() - 0.5F) * 0.045F;
		this.setColor(colorComponent(vx), colorComponent(vy), colorComponent(vz));
		this.pickSprite(spriteSet);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		this.oRoll = this.roll;
		this.roll += this.spinSpeed;

		double drift = 0.00045D;
		this.xd += Math.sin(this.age * 0.17D + this.driftPhase) * drift;
		this.zd += Math.cos(this.age * 0.15D + this.driftPhase) * drift;

		super.tick();
		this.setSpriteFromAge(this.spriteSet);

		float progress = (float) this.age / (float) this.lifetime;
		float fadeIn = smoothstep(0.0F, 0.16F, progress);
		float fadeOut = 1.0F - smoothstep(0.72F, 1.0F, progress);
		this.alpha = 0.60F * fadeIn * fadeOut;
		this.quadSize = this.baseSize * (0.85F + 0.20F * fadeIn);
	}

	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	private static float colorComponent(double value) {
		return Mth.clamp((float) value, 0.0F, 1.0F);
	}

	private static float smoothstep(float edge0, float edge1, float x) {
		if (edge0 == edge1)
			return x < edge0 ? 0.0F : 1.0F;

		x = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}
}
