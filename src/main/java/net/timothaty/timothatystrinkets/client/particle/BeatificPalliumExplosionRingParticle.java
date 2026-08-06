package net.timothaty.timothatystrinkets.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;

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
public final class BeatificPalliumExplosionRingParticle extends TextureSheetParticle {
	private static final int LIFETIME_TICKS = 10;
	private static final float START_RADIUS = 0.0F;
	private static final float END_RADIUS = 4.0F;
	private static final float Y_OFFSET = 0.01F;

	private float previousRadius = START_RADIUS;
	private float radius = START_RADIUS;

	private BeatificPalliumExplosionRingParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
		super(level, x, y + Y_OFFSET, z);
		this.lifetime = LIFETIME_TICKS;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.xd = 0.0D;
		this.yd = 0.0D;
		this.zd = 0.0D;
		this.alpha = 1.0F;
		this.quadSize = 0.01F;
		this.setSize(0.01F, 0.01F);
		this.pickSprite(sprites);
	}

	public static Provider provider(SpriteSet sprites) {
		return new Provider(sprites);
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
		this.previousRadius = this.radius;
		if (++this.age >= this.lifetime) {
			this.remove();
			return;
		}

		float progress = Mth.clamp((float) this.age / this.lifetime, 0.0F, 1.0F);
		float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
		this.radius = Mth.lerp(eased, START_RADIUS, END_RADIUS);
		this.alpha = 1.0F - progress;
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTick) {
		Vec3 cameraPosition = camera.getPosition();
		float centerX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPosition.x());
		float centerY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPosition.y());
		float centerZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPosition.z());
		float renderRadius = Mth.lerp(partialTick, this.previousRadius, this.radius);
		int light = this.getLightColor(partialTick);
		float u0 = this.getU0();
		float u1 = this.getU1();
		float v0 = this.getV0();
		float v1 = this.getV1();

		vertex(buffer, centerX - renderRadius, centerY, centerZ - renderRadius, u0, v0, light, 1.0F);
		vertex(buffer, centerX - renderRadius, centerY, centerZ + renderRadius, u0, v1, light, 1.0F);
		vertex(buffer, centerX + renderRadius, centerY, centerZ + renderRadius, u1, v1, light, 1.0F);
		vertex(buffer, centerX + renderRadius, centerY, centerZ - renderRadius, u1, v0, light, 1.0F);

		vertex(buffer, centerX + renderRadius, centerY, centerZ - renderRadius, u1, v0, light, -1.0F);
		vertex(buffer, centerX + renderRadius, centerY, centerZ + renderRadius, u1, v1, light, -1.0F);
		vertex(buffer, centerX - renderRadius, centerY, centerZ + renderRadius, u0, v1, light, -1.0F);
		vertex(buffer, centerX - renderRadius, centerY, centerZ - renderRadius, u0, v0, light, -1.0F);
	}

	private void vertex(VertexConsumer buffer, float x, float y, float z, float u, float v, int light, float normalY) {
		buffer.addVertex(x, y, z)
				.setUv(u, v)
				.setColor(this.rCol, this.gCol, this.bCol, this.alpha)
				.setLight(light)
				.setNormal(0.0F, normalY, 0.0F);
	}

	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	@OnlyIn(Dist.CLIENT)
	public static final class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		private Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
				double xSpeed, double ySpeed, double zSpeed) {
			return new BeatificPalliumExplosionRingParticle(level, x, y, z, this.sprites);
		}
	}
}
