package net.timothaty.timothatystrinkets.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.wrath_of_the_wicked.WrathOfTheWickedData;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WickedPulseParticle extends TextureSheetParticle {
	private static final float Y_OFFSET = 0.0025F;
	private static final float MAX_ALPHA = 0.88F;

	private final float initialAbilityElapsed;

	public static Provider provider(SpriteSet spriteSet) {
		return new Provider(spriteSet);
	}

	protected WickedPulseParticle(
			ClientLevel level,
			double x,
			double y,
			double z,
			double initialAbilityElapsed,
			SpriteSet spriteSet
	) {
		super(level, x, y + Y_OFFSET, z);
		this.initialAbilityElapsed = (float) initialAbilityElapsed;
		this.lifetime = Math.max(
				1,
				(int) Math.ceil(
						WrathOfTheWickedData.PULSE_VISUAL_START_TICK
								+ WrathOfTheWickedData.PULSE_DURATION_TICKS
								- this.initialAbilityElapsed
				) + 1
		);
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.xd = 0.0D;
		this.yd = 0.0D;
		this.zd = 0.0D;
		this.quadSize = 0.01F;
		this.setSize(0.01F, 0.01F);
		this.pickSprite(spriteSet);
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
		this.age++;
		if (this.age >= this.lifetime)
			this.remove();
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTick) {
		float elapsed = this.initialAbilityElapsed + this.age + partialTick;
		float progress = WrathOfTheWickedData.pulseProgress(elapsed);
		if (elapsed < WrathOfTheWickedData.PULSE_VISUAL_START_TICK
				|| progress >= 1.0F) {
			return;
		}

		float radius = (float) WrathOfTheWickedData.pulseRadius(elapsed);
		float fadeIn = Mth.clamp(progress / 0.10F, 0.0F, 1.0F);
		float fadeOutProgress = Mth.clamp((progress - 0.52F) / 0.48F, 0.0F, 1.0F);
		float fadeOut = 1.0F - smoothstep(fadeOutProgress);
		float renderAlpha = MAX_ALPHA * fadeIn * fadeOut;
		Vec3 cameraPosition = camera.getPosition();
		float centerX = (float) (this.x - cameraPosition.x());
		float centerY = (float) (this.y - cameraPosition.y());
		float centerZ = (float) (this.z - cameraPosition.z());
		float u0 = this.getU0();
		float u1 = this.getU1();
		float v0 = this.getV0();
		float v1 = this.getV1();
		int light = this.getLightColor(partialTick);

		addGroundVertex(buffer, centerX, centerY, centerZ, -radius, -radius, u0, v0, light, renderAlpha, 1.0F);
		addGroundVertex(buffer, centerX, centerY, centerZ, -radius, radius, u0, v1, light, renderAlpha, 1.0F);
		addGroundVertex(buffer, centerX, centerY, centerZ, radius, radius, u1, v1, light, renderAlpha, 1.0F);
		addGroundVertex(buffer, centerX, centerY, centerZ, radius, -radius, u1, v0, light, renderAlpha, 1.0F);

		addGroundVertex(buffer, centerX, centerY, centerZ, radius, -radius, u1, v0, light, renderAlpha, -1.0F);
		addGroundVertex(buffer, centerX, centerY, centerZ, radius, radius, u1, v1, light, renderAlpha, -1.0F);
		addGroundVertex(buffer, centerX, centerY, centerZ, -radius, radius, u0, v1, light, renderAlpha, -1.0F);
		addGroundVertex(buffer, centerX, centerY, centerZ, -radius, -radius, u0, v0, light, renderAlpha, -1.0F);
	}

	private void addGroundVertex(
			VertexConsumer buffer,
			float centerX,
			float centerY,
			float centerZ,
			float localX,
			float localZ,
			float u,
			float v,
			int light,
			float renderAlpha,
			float normalY
	) {
		buffer.addVertex(centerX + localX, centerY, centerZ + localZ)
				.setUv(u, v)
				.setColor(this.rCol, this.gCol, this.bCol, renderAlpha)
				.setLight(light)
				.setNormal(0.0F, normalY, 0.0F);
	}

	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	private static float smoothstep(float value) {
		return value * value * (3.0F - 2.0F * value);
	}

	@OnlyIn(Dist.CLIENT)
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public Provider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(
				SimpleParticleType type,
				ClientLevel level,
				double x,
				double y,
				double z,
				double xSpeed,
				double ySpeed,
				double zSpeed
		) {
			return new WickedPulseParticle(level, x, y, z, xSpeed, spriteSet);
		}
	}
}
