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
import net.minecraft.world.phys.Vec3;

import net.timothaty.timothatystrinkets.client.vfx.spark.SparkTrailHandler;

@OnlyIn(Dist.CLIENT)
public class SparkParticle extends TextureSheetParticle {
	private static final float SPARK_RED = 1.0F;
	private static final float SPARK_GREEN = 106.0F / 255.0F;
	private static final float SPARK_BLUE = 0.0F;
	private final float baseSize;
	private final SparkTrailHandler.SparkTrail trail;

	public static SparkParticleProvider provider(SpriteSet spriteSet) {
		return new SparkParticleProvider(spriteSet);
	}

	public static class SparkParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public SparkParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new SparkParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

	private final SpriteSet spriteSet;

	protected SparkParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(world, x, y, z);
		this.spriteSet = spriteSet;
		this.baseSize = 0.055F + this.random.nextFloat() * 0.025F;
		this.quadSize = this.baseSize;
		this.setSize(0.035F, 0.035F);
		this.lifetime = 12 + this.random.nextInt(5);
		this.gravity = 0.52F;
		this.friction = 0.91F;
		this.hasPhysics = true;
		this.xd = vx;
		this.yd = vy;
		this.zd = vz;
		this.rCol = SPARK_RED;
		this.gCol = SPARK_GREEN;
		this.bCol = SPARK_BLUE;
		this.alpha = 0.95F;
		this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);
		this.oRoll = this.roll;
		Vec3 initialPosition = new Vec3(x, y, z);
		this.trail = SparkTrailHandler.create(initialPosition);
		this.pickSprite(spriteSet);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		this.oRoll = this.roll;
		this.roll += 0.34F + this.random.nextFloat() * 0.08F;
		super.tick();
		if (this.removed) {
			this.trail.stopRecording();
			return;
		}
		if (this.onGround) {
			this.trail.stopRecording();
			this.remove();
			return;
		}

		this.trail.record(new Vec3(this.xo, this.yo, this.zo), new Vec3(this.x, this.y, this.z));
		float progress = Mth.clamp((float) this.age / (float) this.lifetime, 0.0F, 1.0F);
		float fadeOut = 1.0F - progress * progress;
		this.alpha = 0.95F * fadeOut;
		this.quadSize = this.baseSize * (0.72F + 0.28F * fadeOut);
	}

	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}
}
