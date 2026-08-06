package net.timothaty.timothatystrinkets.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@OnlyIn(Dist.CLIENT)
public class RunicFertilizerParticle extends TextureSheetParticle {
	private static final float BASE_SIZE = 0.43F;
	private static final int LIFETIME_TICKS = 40;
	private static final float MAX_ALPHA = 0.72F;
	private static final float TWO_PI = (float) (Math.PI * 2.0D);

	public static RunicFertilizerParticleProvider provider(SpriteSet spriteSet) {
		return new RunicFertilizerParticleProvider(spriteSet);
	}

	public static class RunicFertilizerParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public RunicFertilizerParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new RunicFertilizerParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

	private final SpriteSet spriteSet;
	private final float yaw;

	protected RunicFertilizerParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(world, x, y, z);
		this.spriteSet = spriteSet;
		this.setSize(0.01F, 0.01F);
		this.lifetime = LIFETIME_TICKS;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.xd = 0.0D;
		this.yd = 0.0D;
		this.zd = 0.0D;
		this.quadSize = BASE_SIZE;
		this.alpha = 0.0F;
		this.yaw = this.random.nextFloat() * TWO_PI;
		this.pickSprite(spriteSet);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		super.tick();
		this.setSpriteFromAge(this.spriteSet);

		float progress = this.lifetime <= 1 ? 1.0F : (float) this.age / (float) (this.lifetime - 1);
		float fadeIn = smoothstep(0.0F, 0.12F, progress);
		float fadeOut = 1.0F - smoothstep(0.58F, 1.0F, progress);
		this.alpha = MAX_ALPHA * fadeIn * fadeOut;
		this.quadSize = BASE_SIZE * (0.96F + 0.04F * fadeIn);
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
		Vec3 camPos = camera.getPosition();

		float cx = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
		float cy = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
		float cz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

		float size = this.getQuadSize(partialTicks);
		float sin = Mth.sin(this.yaw);
		float cos = Mth.cos(this.yaw);

		float x0 = -size;
		float z0 = -size;
		float x1 = -size;
		float z1 = size;
		float x2 = size;
		float z2 = size;
		float x3 = size;
		float z3 = -size;

		float rx0 = x0 * cos - z0 * sin;
		float rz0 = x0 * sin + z0 * cos;
		float rx1 = x1 * cos - z1 * sin;
		float rz1 = x1 * sin + z1 * cos;
		float rx2 = x2 * cos - z2 * sin;
		float rz2 = x2 * sin + z2 * cos;
		float rx3 = x3 * cos - z3 * sin;
		float rz3 = x3 * sin + z3 * cos;

		int light = this.getLightColor(partialTicks);
		float u0 = this.getU0();
		float u1 = this.getU1();
		float v0 = this.getV0();
		float v1 = this.getV1();

		renderFace(buffer, cx, cy + 0.001F, cz, rx0, rz0, rx1, rz1, rx2, rz2, rx3, rz3, u0, u1, v0, v1, light, false);
		renderFace(buffer, cx, cy - 0.001F, cz, rx0, rz0, rx1, rz1, rx2, rz2, rx3, rz3, u0, u1, v0, v1, light, true);
	}

	private void renderFace(VertexConsumer buffer, float cx, float cy, float cz, float rx0, float rz0, float rx1, float rz1, float rx2, float rz2, float rx3, float rz3, float u0, float u1, float v0, float v1, int light, boolean underside) {
		if (!underside) {
			addVertex(buffer, cx + rx0, cy, cz + rz0, u1, v1, light);
			addVertex(buffer, cx + rx1, cy, cz + rz1, u1, v0, light);
			addVertex(buffer, cx + rx2, cy, cz + rz2, u0, v0, light);
			addVertex(buffer, cx + rx3, cy, cz + rz3, u0, v1, light);
			return;
		}

		addVertex(buffer, cx + rx3, cy, cz + rz3, u0, v1, light);
		addVertex(buffer, cx + rx2, cy, cz + rz2, u0, v0, light);
		addVertex(buffer, cx + rx1, cy, cz + rz1, u1, v0, light);
		addVertex(buffer, cx + rx0, cy, cz + rz0, u1, v1, light);
	}

	private void addVertex(VertexConsumer buffer, float x, float y, float z, float u, float v, int light) {
		buffer.addVertex(x, y, z).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
	}

	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	@Override
	public AABB getRenderBoundingBox(float partialTicks) {
		return AABB.INFINITE;
	}

	private static float smoothstep(float edge0, float edge1, float x) {
		if (edge0 == edge1)
			return x < edge0 ? 0.0F : 1.0F;

		x = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}
}
