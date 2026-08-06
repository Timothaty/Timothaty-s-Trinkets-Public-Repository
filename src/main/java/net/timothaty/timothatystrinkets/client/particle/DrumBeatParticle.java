package net.timothaty.timothatystrinkets.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.timothaty.timothatystrinkets.util.DrumsOfHasteData;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DrumBeatParticle extends TextureSheetParticle {
	private static final double FEET_Y_OFFSET = 0.02D;

	private final SpriteSet sprites;

	private final float startSize;
	private final float endSize;

	private final float speedMult;
	private final float intensity;

	private final int ownerId;

	protected DrumBeatParticle(ClientLevel level, double x, double y, double z,
			SpriteSet sprites, int ownerId, int stacks) {
		super(level, x, y, z, 0, 0, 0);
		this.sprites = sprites;
		this.ownerId = ownerId;

		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.xd = 0;
		this.yd = 0;
		this.zd = 0;

		this.y += 0.02D;
		this.yo = this.y;

		int visualStacks = Math.min(DrumsOfHasteData.clampStacks(stacks), DrumsOfHasteData.BURST_STACK_CAP);
		float t = visualStacks / (float) DrumsOfHasteData.BURST_STACK_CAP;

		float sizeParam = Mth.lerp(t, 0.45F, 1.45F);
		float speedParam = Mth.lerp(t, 1.15F, 1.95F);
		float intensityParam = Mth.lerp(t, 0.25F, 0.75F);

		float s = Mth.clamp(Math.abs(sizeParam), 0.20F, 3.00F);

		float sp = Math.abs(speedParam);
		this.speedMult = Mth.clamp((sp <= 1.0E-4F ? 1.0F : sp), 0.60F, 2.40F);

		this.intensity = Mth.clamp(Math.abs(intensityParam), 0.0F, 1.0F);

		int baseLife = Mth.clamp((int) Mth.lerp(this.intensity, 28.0F, 82.0F), 20, 90);
		this.lifetime = Mth.clamp((int) Math.round(baseLife / this.speedMult), 12, 90);

		this.startSize = Mth.clamp(s * 0.18F, 0.08F, 0.80F);
		float endBoost = 1.0F + (0.18F * this.intensity);
		this.endSize = Mth.clamp(s * 1.20F * endBoost, 0.55F, 3.20F);

		this.setSpriteFromAge(sprites);
		this.quadSize = this.startSize;
		this.alpha = 0.0F;

		this.rCol = 1.0F;
		this.gCol = 1.0F;
		this.bCol = 1.0F;
	}

	@Override
	public void tick() {
		super.tick();
		followOwnerFeet();
		this.setSpriteFromAge(this.sprites);
	}

	private void followOwnerFeet() {
		Entity owner = this.level.getEntity(this.ownerId);
		if (!(owner instanceof Player player) || player.isRemoved())
			return;

		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		this.x = player.getX();
		this.y = player.getY() + FEET_Y_OFFSET;
		this.z = player.getZ();
	}

	@Override
	public float getQuadSize(float partialTicks) {
		float t = getProgress(partialTicks);
		float eased = easeOutCubic(t);
		return Mth.lerp(eased, this.startSize, this.endSize);
	}

	private float getAlpha(float partialTicks) {
		float t = getProgress(partialTicks);
		float baseAlpha = Mth.lerp(this.intensity, 0.72F, 1.0F);

		float fadeIn = smootherStep(Mth.clamp(t / 0.08F, 0.0F, 1.0F));

		float fadeOut = 1.0F - smootherStep(Mth.clamp((t - 0.12F) / 0.88F, 0.0F, 1.0F));

		return baseAlpha * fadeIn * fadeOut;
	}

	private float getProgress(float partialTicks) {
		return Mth.clamp((this.age + partialTicks) / (float) Math.max(1, this.lifetime - 1), 0.0F, 1.0F);
	}

	private static float easeOutCubic(float t) {
		float inv = 1.0F - t;
		return 1.0F - inv * inv * inv;
	}

	private static float smootherStep(float t) {
		t = Mth.clamp(t, 0.0F, 1.0F);
		return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
	}

	@Override
	public int getLightColor(float partialTick) {
		if (this.intensity >= 0.35F) {
			return 0xF000F0;
		}
		return super.getLightColor(partialTick);
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
		Vec3 camPos = camera.getPosition();

		float cx = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
		float cy = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
		float cz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

		float size = this.getQuadSize(partialTicks);
		float alpha = this.getAlpha(partialTicks);

		float x0 = -size, z0 = -size;
		float x1 = -size, z1 =  size;
		float x2 =  size, z2 =  size;
		float x3 =  size, z3 = -size;

		int light = this.getLightColor(partialTicks);

		float u0 = this.getU0();
		float u1 = this.getU1();
		float v0 = this.getV0();
		float v1 = this.getV1();

		buffer.addVertex(cx + x0, cy, cz + z0).setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, alpha).setLight(light);
		buffer.addVertex(cx + x1, cy, cz + z1).setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, alpha).setLight(light);
		buffer.addVertex(cx + x2, cy, cz + z2).setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, alpha).setLight(light);
		buffer.addVertex(cx + x3, cy, cz + z3).setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, alpha).setLight(light);
	}

	@Override
	public AABB getRenderBoundingBox(float partialTicks) {
		double radius = this.endSize + 1.0D;
		return new AABB(
				this.x - radius, this.y - 0.25D, this.z - radius,
				this.x + radius, this.y + 0.25D, this.z + radius
		);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static Provider provider(SpriteSet sprites) {
		return new Provider(sprites);
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
				double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			int ownerId = Mth.floor(xSpeed);
			int stacks = Mth.floor(ySpeed);
			return new DrumBeatParticle(level, x, y, z, sprites, ownerId, stacks);
		}
	}
}
