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
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class StaggerSpiralParticle extends TextureSheetParticle {
	private static final Map<Integer, StaggerSpiralParticle> ACTIVE_PARTICLES = new HashMap<>();

	public static void clearTrackedParticles() {
		ACTIVE_PARTICLES.clear();
	}

	private static final int SERVER_KEEPALIVE_TIMEOUT_TICKS = 8;
	private static final int FADE_IN_TICKS = 4;
	private static final int FADE_OUT_TICKS = 8;
	private static final int ANIMATION_LENGTH_TICKS = 3;
	private static final float MIN_SIZE = 0.65F;
	private static final float SIZE_MULTIPLIER = 1.15F;
	private static final float HEAD_Y_OFFSET = 0.24F;
	private static final float ROTATION_SPEED_DEGREES_PER_TICK = 7.0F;

	public static StaggerSpiralParticleProvider provider(SpriteSet spriteSet) {
		return new StaggerSpiralParticleProvider(spriteSet);
	}

	public static class StaggerSpiralParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public StaggerSpiralParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			int entityId = Mth.floor(xSpeed);
			if (entityId > 0) {
				Entity owner = worldIn.getEntity(entityId);
				StaggerSpiralParticle active = ACTIVE_PARTICLES.get(entityId);
				if (owner == null || !owner.isAlive()) {
					if (active != null) {
						active.startFadeOut();
					}
					return null;
				}

				if (active != null && active.isStillAlive()) {
					active.refreshFromServerPing(x, y, z, ySpeed);
					return null;
				}
			}
			return new StaggerSpiralParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

	private final SpriteSet spriteSet;
	private final int attachedEntityId;
	private Entity attachedEntity;
	private double extraYOffset;

	private float renderSize = MIN_SIZE;
	private float renderSizeOld = MIN_SIZE;
	private float rotationDegrees;
	private float rotationDegreesOld;
	private float alphaOld;
	private int ticksSinceServerPing;
	private int fadeInTicks;
	private int fadeOutTicks;
	private int fadeOutSpriteAge = -1;
	private boolean fadingOut;

	protected StaggerSpiralParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(world, x, y, z);
		this.spriteSet = spriteSet;
		this.attachedEntityId = Mth.floor(vx);
		this.extraYOffset = Math.max(0.0D, vy);
		this.lifetime = Integer.MAX_VALUE;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.xd = 0.0D;
		this.yd = 0.0D;
		this.zd = 0.0D;
		this.alpha = 0.0F;
		this.rotationDegrees = this.random.nextFloat() * 360.0F;
		this.rotationDegreesOld = this.rotationDegrees;
		this.setSize(0.01F, 0.01F);
		this.pickSprite(spriteSet);

		if (this.attachedEntityId > 0) {
			ACTIVE_PARTICLES.put(this.attachedEntityId, this);
		}

		Entity entity = this.getAttachedEntity();
		if (entity != null) {
			this.syncToEntity(entity);
		} else {
			this.setPos(x, y, z);
		}
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
		this.renderSizeOld = this.renderSize;
		this.rotationDegreesOld = this.rotationDegrees;
		this.alphaOld = this.alpha;

		this.age++;
		this.ticksSinceServerPing++;
		this.fadeInTicks = Math.min(this.fadeInTicks + 1, FADE_IN_TICKS);
		this.rotationDegrees = Mth.wrapDegrees(this.rotationDegrees + ROTATION_SPEED_DEGREES_PER_TICK);

		Entity entity = this.getAttachedEntity();
		if (this.fadingOut || entity == null || !entity.isAlive() || this.ticksSinceServerPing > SERVER_KEEPALIVE_TIMEOUT_TICKS) {
			this.startFadeOut();
		} else {
			this.syncToEntity(entity);
			this.stopFadeOut();
		}

		int spriteAge = this.fadingOut && this.fadeOutSpriteAge >= 0 ? this.fadeOutSpriteAge : this.age;
		this.setSprite(this.spriteSet.get(spriteAge % ANIMATION_LENGTH_TICKS, ANIMATION_LENGTH_TICKS));
		this.updateAlphaAndShrink();

		if (this.fadingOut && this.fadeOutTicks >= FADE_OUT_TICKS) {
			this.remove();
		}
	}

	private void refreshFromServerPing(double x, double y, double z, double yOffset) {
		if (this.fadingOut)
			return;

		this.ticksSinceServerPing = 0;
		this.extraYOffset = Math.max(0.0D, yOffset);
		Entity entity = this.getAttachedEntity();
		if (entity == null || !entity.isAlive()) {
			this.startFadeOut();
			return;
		}

		this.syncToEntity(entity);
	}

	private void syncToEntity(Entity entity) {
		this.setPos(entity.getX(), entity.getY() + entity.getBbHeight() + HEAD_Y_OFFSET + this.extraYOffset, entity.getZ());
		float widthBased = entity.getBbWidth() * SIZE_MULTIPLIER;
		float heightBased = entity.getBbHeight() * 0.22F;
		this.renderSize = Math.max(MIN_SIZE, Math.max(widthBased, heightBased));
	}

	private void updateAlphaAndShrink() {
		float fadeInAlpha = smoothStep(Mth.clamp((float) this.fadeInTicks / (float) FADE_IN_TICKS, 0.0F, 1.0F));
		float fadeOutAlpha = 1.0F;
		if (this.fadingOut) {
			this.fadeOutTicks++;
			fadeOutAlpha = 1.0F - smoothStep(Mth.clamp((float) this.fadeOutTicks / (float) FADE_OUT_TICKS, 0.0F, 1.0F));
			this.renderSize *= fadeOutAlpha;
		}
		this.alpha = fadeInAlpha * fadeOutAlpha;
	}

	private static float smoothStep(float value) {
		float x = Mth.clamp(value, 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}

	private void startFadeOut() {
		if (!this.fadingOut) {
			this.fadingOut = true;
			this.fadeOutTicks = 0;
			this.fadeOutSpriteAge = this.age;
		}
	}

	private void stopFadeOut() {
		this.fadingOut = false;
		this.fadeOutTicks = 0;
		this.fadeOutSpriteAge = -1;
	}

	private Entity getAttachedEntity() {
		if (this.attachedEntity != null && this.attachedEntity.isAlive()) {
			return this.attachedEntity;
		}
		if (this.attachedEntityId <= 0) {
			return null;
		}
		this.attachedEntity = this.level.getEntity(this.attachedEntityId);
		return this.attachedEntity;
	}

	private boolean isStillAlive() {
		return !this.removed;
	}

	@Override
	public void remove() {
		super.remove();
		StaggerSpiralParticle active = ACTIVE_PARTICLES.get(this.attachedEntityId);
		if (active == this) {
			ACTIVE_PARTICLES.remove(this.attachedEntityId);
		}
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
		Vec3 cameraPos = camera.getPosition();
		float particleX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
		float particleY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
		float particleZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

		float halfSize = Mth.lerp(partialTicks, this.renderSizeOld, this.renderSize) * 0.5F;
		float selfRotation = Mth.rotLerp(partialTicks, this.rotationDegreesOld, this.rotationDegrees);
		float renderAlpha = Mth.lerp(partialTicks, this.alphaOld, this.alpha);
		if (renderAlpha <= 0.001F)
			return;
		float yawRadians = selfRotation * ((float) Math.PI / 180.0F);
		float cos = Mth.cos(yawRadians);
		float sin = Mth.sin(yawRadians);

		float u0 = this.getU0();
		float u1 = this.getU1();
		float v0 = this.getV0();
		float v1 = this.getV1();
		int light = this.getLightColor(partialTicks);

		addHorizontalVertex(buffer, particleX, particleY, particleZ, -halfSize, -halfSize, cos, sin, u0, v0, light, 1.0F, renderAlpha);
		addHorizontalVertex(buffer, particleX, particleY, particleZ, -halfSize, halfSize, cos, sin, u0, v1, light, 1.0F, renderAlpha);
		addHorizontalVertex(buffer, particleX, particleY, particleZ, halfSize, halfSize, cos, sin, u1, v1, light, 1.0F, renderAlpha);
		addHorizontalVertex(buffer, particleX, particleY, particleZ, halfSize, -halfSize, cos, sin, u1, v0, light, 1.0F, renderAlpha);

		addHorizontalVertex(buffer, particleX, particleY, particleZ, halfSize, -halfSize, cos, sin, u1, v0, light, -1.0F, renderAlpha);
		addHorizontalVertex(buffer, particleX, particleY, particleZ, halfSize, halfSize, cos, sin, u1, v1, light, -1.0F, renderAlpha);
		addHorizontalVertex(buffer, particleX, particleY, particleZ, -halfSize, halfSize, cos, sin, u0, v1, light, -1.0F, renderAlpha);
		addHorizontalVertex(buffer, particleX, particleY, particleZ, -halfSize, -halfSize, cos, sin, u0, v0, light, -1.0F, renderAlpha);
	}

	private void addHorizontalVertex(VertexConsumer buffer, float centerX, float centerY, float centerZ, float localX, float localZ, float cos, float sin, float u, float v, int light, float normalY, float renderAlpha) {
		float rotatedX = localX * cos - localZ * sin;
		float rotatedZ = localX * sin + localZ * cos;
		buffer.addVertex(centerX + rotatedX, centerY, centerZ + rotatedZ)
				.setUv(u, v)
				.setColor(this.rCol, this.gCol, this.bCol, renderAlpha)
				.setLight(light)
				.setNormal(0.0F, normalY, 0.0F);
	}

	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}
}
