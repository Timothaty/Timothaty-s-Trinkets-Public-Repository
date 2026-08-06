package net.timothaty.timothatystrinkets.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@OnlyIn(Dist.CLIENT)
public class SacrificeFailedParticle extends TextureSheetParticle {
	private final SpriteSet sprites;

	private final double baseX;
	private final double baseY;
	private final double baseZ;

	private final float halfSize;
	private final float riseHeight;

	public static SacrificeFailedParticleProvider provider(SpriteSet spriteSet) {
		return new SacrificeFailedParticleProvider(spriteSet);
	}

	protected SacrificeFailedParticle(ClientLevel level, double x, double y, double z,
			double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
		super(level, x, y, z, 0, 0, 0);

		this.sprites = sprites;
		this.setSpriteFromAge(sprites);
		this.hasPhysics = false;
		this.gravity = 0.0F;
		this.baseX = x;
		this.baseY = y;
		this.baseZ = z;
		this.lifetime = 25 + this.random.nextInt(10);
		this.halfSize = 0.74F; 
		this.riseHeight = 0.9F + this.random.nextFloat() * 0.35F;
		this.alpha = 1.0F;
		this.xd = 0;
		this.yd = 0;
		this.zd = 0;
		this.quadSize = 1.0F;
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

		this.setSpriteFromAge(this.sprites);

		float t = (float) this.age / (float) this.lifetime; 
		float ease = 1.0F - (float) Math.pow(1.0F - t, 3.0);

		this.x = this.baseX;
		this.y = this.baseY + (this.riseHeight * ease);
		this.z = this.baseZ;
		this.quadSize = 0.96F + 0.08F * Mth.sin(t * Mth.PI);
		this.alpha = 1.0F - t;
	}

	@Override
	public void render(VertexConsumer vc, Camera camera, float partialTick) {
		Vec3 camPos = camera.getPosition();

		float cx = (float) (Mth.lerp(partialTick, this.xo, this.x) - camPos.x);
		float cy = (float) (Mth.lerp(partialTick, this.yo, this.y) - camPos.y);
		float cz = (float) (Mth.lerp(partialTick, this.zo, this.z) - camPos.z);

		float size = this.halfSize * this.getQuadSize(partialTick);

		float x1 = cx - size, z1 = cz - size;
		float x2 = cx - size, z2 = cz + size;
		float x3 = cx + size, z3 = cz + size;
		float x4 = cx + size, z4 = cz - size;

		float u0 = this.getU0();
		float u1 = this.getU1();
		float v0 = this.getV0();
		float v1 = this.getV1();

		int light = this.getLightColor(partialTick);

		vc.addVertex(x1, cy, z1).setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
		vc.addVertex(x2, cy, z2).setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
		vc.addVertex(x3, cy, z3).setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
		vc.addVertex(x4, cy, z4).setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	protected int getLightColor(float partialTick) {
		return LightTexture.pack(15, 15);
	}

	@OnlyIn(Dist.CLIENT)
	public static class SacrificeFailedParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public SacrificeFailedParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
				double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new SacrificeFailedParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}
}