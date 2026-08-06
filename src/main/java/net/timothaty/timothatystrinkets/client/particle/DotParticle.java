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
public class DotParticle extends TextureSheetParticle {
	public static DotParticleProvider provider(SpriteSet spriteSet) {
		return new DotParticleProvider(spriteSet);
	}

	public static class DotParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public DotParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new DotParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

	private final SpriteSet spriteSet;
	private final float baseSize;
	private final boolean energyMode;

	protected DotParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(world, x, y, z);
		this.spriteSet = spriteSet;
		this.energyMode = isColorComponent(vx) && isColorComponent(vy) && isColorComponent(vz);
		this.gravity = 0f;
		this.pickSprite(spriteSet);
		if (this.energyMode) {
			this.baseSize = 0.035F + this.random.nextFloat() * 0.030F;
			this.quadSize = this.baseSize;
			this.setSize(0.02F, 0.02F);
			this.lifetime = 24 + this.random.nextInt(15);
			this.hasPhysics = false;
			this.friction = 0.965F;
			this.alpha = 0.0F;
			this.xd = (this.random.nextDouble() - 0.5D) * 0.010D;
			this.yd = 0.024D + this.random.nextDouble() * 0.018D;
			this.zd = (this.random.nextDouble() - 0.5D) * 0.010D;
			this.setColor((float) vx, (float) vy, (float) vz);
			this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);
			this.oRoll = this.roll;
		} else {
			this.baseSize = 0.2F;
			this.setSize(0.2f, 0.2f);
			this.lifetime = 7;
			this.hasPhysics = true;
			this.xd = vx * 1;
			this.yd = vy * 1;
			this.zd = vz * 1;
		}
	}

	@Override
	public ParticleRenderType getRenderType() {
		return this.energyMode ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT : ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}

	@Override
	public void tick() {
		this.oRoll = this.roll;
		if (this.energyMode) {
			this.roll += 0.045F;
			this.xd += Math.sin((this.age + this.random.nextFloat()) * 0.28D) * 0.00065D;
			this.zd += Math.cos((this.age + this.random.nextFloat()) * 0.24D) * 0.00065D;
		}

		super.tick();
		if (!this.energyMode || this.removed)
			return;

		this.setSpriteFromAge(this.spriteSet);
		float progress = (float) this.age / (float) this.lifetime;
		float fadeIn = smoothstep(0.0F, 0.18F, progress);
		float fadeOut = 1.0F - smoothstep(0.68F, 1.0F, progress);
		this.alpha = 0.80F * fadeIn * fadeOut;
		this.quadSize = this.baseSize * (0.75F + 0.42F * fadeIn);
	}

	@Override
	public int getLightColor(float partialTick) {
		return this.energyMode ? 0xF000F0 : super.getLightColor(partialTick);
	}

	private static boolean isColorComponent(double value) {
		return value >= 0.0D && value <= 1.0D;
	}

	private static float smoothstep(float edge0, float edge1, float x) {
		if (edge0 == edge1)
			return x < edge0 ? 0.0F : 1.0F;

		x = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}
}
