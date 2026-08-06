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
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DeadLuckParticle extends TextureSheetParticle {
	public static DeadLuckParticleProvider provider(SpriteSet spriteSet) {
		return new DeadLuckParticleProvider(spriteSet);
	}

	private final SpriteSet sprites;

	private final Quaternionf baseOrientation;
	private final Vector3f spinAxis;

	private final float spinSpeed;
	private final float wobbleOffset;
	private final float baseSize;

	protected DeadLuckParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, 0, 0, 0);

		this.sprites = sprites;
		this.setSpriteFromAge(sprites);

		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.friction = 0.92F;

		this.lifetime = 32 + this.random.nextInt(28);

		this.baseSize = 0.18F + this.random.nextFloat() * 0.08F;
		this.quadSize = this.baseSize;

		this.xd = xd * 0.10D + (this.random.nextDouble() - 0.5D) * 0.020D;
		this.yd = yd * 0.10D + 0.020D + this.random.nextDouble() * 0.010D;
		this.zd = zd * 0.10D + (this.random.nextDouble() - 0.5D) * 0.020D;

		this.alpha = 0.0F;

		this.rCol = 1.0F;
		this.gCol = 1.0F;
		this.bCol = 1.0F;

		float yaw = this.random.nextFloat() * ((float) Math.PI * 2F);
		float pitch = (this.random.nextFloat() - 0.5F) * 0.55F;
		this.baseOrientation = new Quaternionf().rotateY(yaw).rotateX(pitch);

		this.spinAxis = new Vector3f(0, 0, 1).rotate(this.baseOrientation).normalize();

		this.roll = this.random.nextFloat() * ((float) Math.PI * 2F);
		this.oRoll = this.roll;

		this.spinSpeed = (this.random.nextFloat() - 0.5F) * 0.28F;
		this.wobbleOffset = this.random.nextFloat() * ((float) Math.PI * 2F);
	}

	@Override
	public void tick() {
		this.oRoll = this.roll;
		this.roll += this.spinSpeed;

		float t = (float) this.age / (float) this.lifetime;
		float fadeIn = smoothstep(0.0F, 0.12F, t);
		float fadeOut = 1.0F - smoothstep(0.78F, 1.0F, t);
		this.alpha = Mth.clamp(fadeIn * fadeOut, 0.0F, 1.0F);

		double wobbleX = Math.sin((this.age * 0.25F) + this.wobbleOffset) * 0.0024D;
		double wobbleZ = Math.cos((this.age * 0.23F) + this.wobbleOffset) * 0.0024D;
		this.xd += wobbleX;
		this.zd += wobbleZ;

		this.yd += 0.0015D + (this.random.nextDouble() - 0.5D) * 0.0010D;

		if ((this.age & 1) == 0) {
			this.xd += (this.random.nextDouble() - 0.5D) * 0.0014D;
			this.zd += (this.random.nextDouble() - 0.5D) * 0.0014D;
		}

		float pulse = (float) (Math.sin((this.age * 0.18F) + this.wobbleOffset) * 0.02F);
		this.quadSize = this.baseSize * (1.0F + pulse);

		super.tick();
		this.setSpriteFromAge(this.sprites);
	}

	@Override
	public void render(VertexConsumer vc, Camera camera, float partialTicks) {
		Vec3 camPos = camera.getPosition();

		float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
		float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
		float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

		float angle = Mth.lerp(partialTicks, this.oRoll, this.roll);

		Quaternionf q = new Quaternionf(this.baseOrientation)
				.rotateAxis(angle, this.spinAxis.x, this.spinAxis.y, this.spinAxis.z);

		float size = this.getQuadSize(partialTicks);

		Vector3f v0 = new Vector3f(-1, -1, 0).mul(size).rotate(q);
		Vector3f v1 = new Vector3f(-1,  1, 0).mul(size).rotate(q);
		Vector3f v2 = new Vector3f( 1,  1, 0).mul(size).rotate(q);
		Vector3f v3 = new Vector3f( 1, -1, 0).mul(size).rotate(q);

		float u0 = this.getU0();
		float u1 = this.getU1();
		float vv0 = this.getV0();
		float vv1 = this.getV1();

		int light = this.getLightColor(partialTicks);

		int r = (int) (this.rCol * 255.0F);
		int g = (int) (this.gCol * 255.0F);
		int b = (int) (this.bCol * 255.0F);
		int a = (int) (this.alpha * 255.0F);

		vc.addVertex(px + v0.x, py + v0.y, pz + v0.z).setColor(r, g, b, a).setUv(u0, vv1).setLight(light);
		vc.addVertex(px + v1.x, py + v1.y, pz + v1.z).setColor(r, g, b, a).setUv(u0, vv0).setLight(light);
		vc.addVertex(px + v2.x, py + v2.y, pz + v2.z).setColor(r, g, b, a).setUv(u1, vv0).setLight(light);
		vc.addVertex(px + v3.x, py + v3.y, pz + v3.z).setColor(r, g, b, a).setUv(u1, vv1).setLight(light);

		vc.addVertex(px + v0.x, py + v0.y, pz + v0.z).setColor(r, g, b, a).setUv(u0, vv1).setLight(light);
		vc.addVertex(px + v3.x, py + v3.y, pz + v3.z).setColor(r, g, b, a).setUv(u1, vv1).setLight(light);
		vc.addVertex(px + v2.x, py + v2.y, pz + v2.z).setColor(r, g, b, a).setUv(u1, vv0).setLight(light);
		vc.addVertex(px + v1.x, py + v1.y, pz + v1.z).setColor(r, g, b, a).setUv(u0, vv0).setLight(light);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	private static float smoothstep(float edge0, float edge1, float x) {
		if (edge0 == edge1) return x < edge0 ? 0.0F : 1.0F;
		x = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}

	@OnlyIn(Dist.CLIENT)
	public static class DeadLuckParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public DeadLuckParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
			return new DeadLuckParticle(level, x, y, z, xd, yd, zd, this.spriteSet);
		}
	}
}