package net.timothaty.timothatystrinkets.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Camera;
import net.minecraft.core.particles.SimpleParticleType;
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
public class RoarOfFearParticle extends TextureSheetParticle {
	public static RoarOfFearParticleProvider provider(SpriteSet spriteSet) {
		return new RoarOfFearParticleProvider(spriteSet);
	}

	public static class RoarOfFearParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public RoarOfFearParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new RoarOfFearParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

	private final SpriteSet spriteSet;
	private final float forwardX;
	private final float forwardZ;
	private final float rightX;
	private final float rightZ;
	private final float baseSize;
	private final float travelSpeed;
	private final float tilt;

	protected RoarOfFearParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(world, x, y, z);
		this.spriteSet = spriteSet;
		double horizontalLength = Math.sqrt(vx * vx + vz * vz);
		if (horizontalLength < 1.0E-4D) {
			float randomYaw = world.random.nextFloat() * Mth.TWO_PI;
			this.forwardX = Mth.cos(randomYaw);
			this.forwardZ = Mth.sin(randomYaw);
		} else {
			this.forwardX = (float) (vx / horizontalLength);
			this.forwardZ = (float) (vz / horizontalLength);
		}
		this.rightX = -this.forwardZ;
		this.rightZ = this.forwardX;
		this.baseSize = vy > 0.0D ? Mth.clamp((float) vy, 0.28F, 1.05F) : 0.58F;
		this.travelSpeed = Mth.clamp((float) horizontalLength, 0.16F, 0.55F);
		this.tilt = (world.random.nextFloat() - 0.5F) * 0.18F;
		this.setSize(0.01F, 0.01F);
		this.lifetime = 15 + world.random.nextInt(5);
		this.gravity = 0.0F;
		this.hasPhysics = false;
		double driftSpeed = this.travelSpeed * (0.92D + world.random.nextDouble() * 0.18D);
		this.xd = this.forwardX * driftSpeed;
		this.yd = (world.random.nextDouble() - 0.5D) * 0.004D;
		this.zd = this.forwardZ * driftSpeed;
		this.rCol = 0.95F;
		this.gCol = 0.02F;
		this.bCol = 0.08F;
		this.alpha = 0.82F;
		this.quadSize = this.baseSize * 0.18F;
		this.pickSprite(spriteSet);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		super.tick();
		float progress = this.lifetime <= 1 ? 1.0F : Mth.clamp(this.age / (float) (this.lifetime - 1), 0.0F, 1.0F);
		float fade = 1.0F - progress;
		float growth = 1.0F - (1.0F - progress) * (1.0F - progress);
		this.alpha = 0.82F * fade * fade;
		this.quadSize = this.baseSize * (0.18F + growth * 1.45F);
		this.setSpriteFromAge(this.spriteSet);
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
		Vec3 camPos = camera.getPosition();
		float cx = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
		float cy = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
		float cz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);
		float width = this.getQuadSize(partialTicks);
		float height = width * 1.65F;
		float bottomY = -height * 0.48F;
		float topY = height * 0.52F;
		float bottomForward = -this.tilt * 0.35F;
		float topForward = this.tilt;

		float leftX = -this.rightX * width;
		float leftZ = -this.rightZ * width;
		float rightXValue = this.rightX * width;
		float rightZValue = this.rightZ * width;
		float bottomForwardX = this.forwardX * bottomForward;
		float bottomForwardZ = this.forwardZ * bottomForward;
		float topForwardX = this.forwardX * topForward;
		float topForwardZ = this.forwardZ * topForward;

		int light = this.getLightColor(partialTicks);
		float u0 = this.getU0();
		float u1 = this.getU1();
		float v0 = this.getV0();
		float v1 = this.getV1();

		float x0 = cx + leftX + bottomForwardX;
		float y0 = cy + bottomY;
		float z0 = cz + leftZ + bottomForwardZ;
		float x1 = cx + leftX + topForwardX;
		float y1 = cy + topY;
		float z1 = cz + leftZ + topForwardZ;
		float x2 = cx + rightXValue + topForwardX;
		float y2 = cy + topY;
		float z2 = cz + rightZValue + topForwardZ;
		float x3 = cx + rightXValue + bottomForwardX;
		float y3 = cy + bottomY;
		float z3 = cz + rightZValue + bottomForwardZ;

		addVertex(buffer, x0, y0, z0, u0, v1, light);
		addVertex(buffer, x1, y1, z1, u0, v0, light);
		addVertex(buffer, x2, y2, z2, u1, v0, light);
		addVertex(buffer, x3, y3, z3, u1, v1, light);

		addVertex(buffer, x3, y3, z3, u1, v1, light);
		addVertex(buffer, x2, y2, z2, u1, v0, light);
		addVertex(buffer, x1, y1, z1, u0, v0, light);
		addVertex(buffer, x0, y0, z0, u0, v1, light);
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
		double width = this.getQuadSize(partialTicks);
		double height = width * 1.65D;
		double horizontalRadius = width * 1.25D + 0.25D;
		return new AABB(
			this.x - horizontalRadius,
			this.y - height * 0.55D - 0.1D,
			this.z - horizontalRadius,
			this.x + horizontalRadius,
			this.y + height * 0.55D + 0.1D,
			this.z + horizontalRadius);
	}
}
