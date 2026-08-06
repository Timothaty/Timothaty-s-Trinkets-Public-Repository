package net.timothaty.timothatystrinkets.client.particle;

import net.timothaty.timothatystrinkets.client.vfx.spark.SparkTrailHandler;
import net.timothaty.timothatystrinkets.particle.TintedShardParticleOptions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class TintedShardParticle extends TextureSheetParticle {
	private static final float TRAIL_CHANCE = 0.25F;
	private static final float BASE_ALPHA = 0.95F;

	private final float baseSize;
	private final float rollSpeed;
	private final SparkTrailHandler.SparkTrail trail;

	private TintedShardParticle(ClientLevel level, double x, double y, double z,
			double xSpeed, double ySpeed, double zSpeed,
			TintedShardParticleOptions options, SpriteSet sprites) {
		super(level, x, y, z);
		Vector3f color = options.color();
		this.baseSize = options.scale();
		this.quadSize = this.baseSize;
		this.setSize(this.baseSize, this.baseSize);
		this.lifetime = 16 + this.random.nextInt(9);
		this.gravity = 0.38F + this.random.nextFloat() * 0.12F;
		this.friction = 0.92F;
		this.hasPhysics = true;
		this.xd = xSpeed;
		this.yd = ySpeed;
		this.zd = zSpeed;
		this.rCol = color.x();
		this.gCol = color.y();
		this.bCol = color.z();
		this.alpha = BASE_ALPHA;
		this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);
		this.oRoll = this.roll;
		float rotationDirection = this.random.nextBoolean() ? 1.0F : -1.0F;
		this.rollSpeed = rotationDirection * (0.32F + this.random.nextFloat() * 0.20F);
		this.trail = this.random.nextFloat() < TRAIL_CHANCE
				? SparkTrailHandler.create(
						new Vec3(x, y, z),
						Math.round(color.x() * 255.0F),
						Math.round(color.y() * 255.0F),
						Math.round(color.z() * 255.0F)
				)
				: null;
		this.pickSprite(sprites);
	}

	public static ParticleProvider<TintedShardParticleOptions> provider(SpriteSet sprites) {
		return (options, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
				new TintedShardParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options, sprites);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		this.oRoll = this.roll;
		this.roll += this.rollSpeed;
		super.tick();
		if (this.removed) {
			stopTrail();
			return;
		}
		if (this.onGround) {
			stopTrail();
			this.remove();
			return;
		}

		if (this.trail != null)
			this.trail.record(new Vec3(this.xo, this.yo, this.zo), new Vec3(this.x, this.y, this.z));

		float progress = Mth.clamp((float) this.age / (float) this.lifetime, 0.0F, 1.0F);
		float fadeProgress = Mth.clamp((progress - 0.55F) / 0.45F, 0.0F, 1.0F);
		float fade = 1.0F - fadeProgress * fadeProgress;
		this.alpha = BASE_ALPHA * fade;
		this.quadSize = this.baseSize * (1.0F - progress * 0.28F);
	}

	@Override
	public void remove() {
		stopTrail();
		super.remove();
	}

	private void stopTrail() {
		if (this.trail != null)
			this.trail.stopRecording();
	}

	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}
}
