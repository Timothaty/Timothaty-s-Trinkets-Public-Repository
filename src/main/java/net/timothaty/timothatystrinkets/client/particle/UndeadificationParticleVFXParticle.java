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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@OnlyIn(Dist.CLIENT)
public class UndeadificationParticleVFXParticle extends TextureSheetParticle {
	private final SpriteSet sprites;

	private LivingEntity sourceEntity;

	private final double baseX;
	private final double baseY;
	private final double baseZ;

	private float startHalfSizeX;
	private float startHalfSizeZ;
	private float endHalfSizeX;
	private float endHalfSizeZ;

	private float halfSizeX;
	private float halfSizeZ;

	private float maxRise;
	private float bodyYawRad;

	public static UndeadificationParticleVFXParticleProvider provider(SpriteSet spriteSet) {
		return new UndeadificationParticleVFXParticleProvider(spriteSet);
	}

	protected UndeadificationParticleVFXParticle(ClientLevel level, double x, double y, double z,
			double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
		super(level, x, y, z, 0.0D, 0.0D, 0.0D);

		this.sprites = sprites;
		this.setSpriteFromAge(sprites);

		this.hasPhysics = false;
		this.gravity = 0.0F;

		this.baseX = x;
		this.baseY = y;
		this.baseZ = z;

		this.lifetime = 24 + this.random.nextInt(8);

		this.quadSize = 1.0F;
		this.alpha = 0.0F;

		this.xd = 0.0D;
		this.yd = 0.0D;
		this.zd = 0.0D;

		this.rCol = 1.0F;
		this.gCol = 1.0F;
		this.bCol = 1.0F;

		this.sourceEntity = this.findSourceEntity();
		this.resolveVisualParameters();

		this.halfSizeX = this.startHalfSizeX;
		this.halfSizeZ = this.startHalfSizeZ;
	}

	private LivingEntity findSourceEntity() {
		AABB searchBox = new AABB(this.baseX, this.baseY, this.baseZ, this.baseX, this.baseY, this.baseZ).inflate(2.0D, 1.5D, 2.0D);
		LivingEntity closest = null;
		double closestDistanceSq = Double.MAX_VALUE;

		for (LivingEntity candidate : this.level.getEntitiesOfClass(LivingEntity.class, searchBox, LivingEntity::isAlive)) {
			double distanceSq = candidate.distanceToSqr(this.baseX, this.baseY, this.baseZ);
			if (distanceSq < closestDistanceSq) {
				closestDistanceSq = distanceSq;
				closest = candidate;
			}
		}

		return closest;
	}

	private void resolveVisualParameters() {
	if (this.sourceEntity != null && this.sourceEntity.isAlive()) {
		float width = Math.max(this.sourceEntity.getBbWidth(), 0.6F);
		float height = Math.max(this.sourceEntity.getBbHeight(), 0.8F);

	this.startHalfSizeX = width * 0.55F;
this.startHalfSizeZ = width * 0.55F;
this.endHalfSizeX = width * 1.15F;
this.endHalfSizeZ = width * 1.15F;

		this.maxRise = height;

		this.bodyYawRad = this.sourceEntity.yBodyRot * Mth.DEG_TO_RAD;
	} else {
		this.startHalfSizeX = 0.42F;
		this.startHalfSizeZ = 0.42F;
		this.endHalfSizeX = 0.72F;
		this.endHalfSizeZ = 0.72F;
		this.maxRise = 1.2F;
		this.bodyYawRad = 0.0F;
	}
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

		double ringBaseY;
		if (this.sourceEntity != null && this.sourceEntity.isAlive()) {
			this.resolveVisualParameters();

			this.x = this.sourceEntity.getX();
			this.z = this.sourceEntity.getZ();
			ringBaseY = this.sourceEntity.getY() + 0.04D;
		} else {
			this.x = this.baseX;
			this.z = this.baseZ;
			ringBaseY = this.baseY;
		}

		float t = (float) this.age / (float) this.lifetime;

		float expandEase = 1.0F - (float) Math.pow(1.0F - t, 2.0D);

		float riseEase = t * t * (3.0F - 2.0F * t);

		this.halfSizeX = Mth.lerp(expandEase, this.startHalfSizeX, this.endHalfSizeX);
		this.halfSizeZ = Mth.lerp(expandEase, this.startHalfSizeZ, this.endHalfSizeZ);

		this.y = ringBaseY + this.maxRise * riseEase;

		this.quadSize = 0.98F + 0.04F * Mth.sin(t * Mth.PI);

		float fadeIn = Mth.clamp(t * 5.0F, 0.0F, 1.0F);
		float fadeOut = 1.0F - t;
		this.alpha = fadeIn * fadeOut;
	}

	@Override
	public void render(VertexConsumer vc, Camera camera, float partialTick) {
		Vec3 camPos = camera.getPosition();

		float cx = (float) (Mth.lerp(partialTick, this.xo, this.x) - camPos.x);
		float cy = (float) (Mth.lerp(partialTick, this.yo, this.y) - camPos.y);
		float cz = (float) (Mth.lerp(partialTick, this.zo, this.z) - camPos.z);

		float sizeMultiplier = this.getQuadSize(partialTick);
		float hx = this.halfSizeX * sizeMultiplier;
		float hz = this.halfSizeZ * sizeMultiplier;

		float sin = Mth.sin(this.bodyYawRad);
		float cos = Mth.cos(this.bodyYawRad);

		float forwardX = -sin;
		float forwardZ = cos;
		float rightX = cos;
		float rightZ = sin;

		float x1 = cx - rightX * hx - forwardX * hz;
		float z1 = cz - rightZ * hx - forwardZ * hz;
		float x2 = cx - rightX * hx + forwardX * hz;
		float z2 = cz - rightZ * hx + forwardZ * hz;
		float x3 = cx + rightX * hx + forwardX * hz;
		float z3 = cz + rightZ * hx + forwardZ * hz;
		float x4 = cx + rightX * hx - forwardX * hz;
		float z4 = cz + rightZ * hx - forwardZ * hz;

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
	public static class UndeadificationParticleVFXParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public UndeadificationParticleVFXParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
				double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new UndeadificationParticleVFXParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}
}