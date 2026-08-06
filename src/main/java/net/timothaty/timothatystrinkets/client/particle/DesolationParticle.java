package net.timothaty.timothatystrinkets.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.timothaty.timothatystrinkets.potion.DesolatedMobEffect;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class DesolationParticle extends TextureSheetParticle {
	private static final float SIZE_MULTIPLIER = 2.90f;
	private static final float MIN_SIZE = 1.50f;
	private static final float CULLING_DIAGONAL_MULTIPLIER = 1.41421356f;
	private static final float CULLING_HEIGHT = 0.25f;
	private static final float CULLING_SIZE_EPSILON = 1.0E-4f;
	private static final float Y_OFFSET = 0.025f;

	private static final float ROTATION_OFFSET_DEGREES = 0f;

	private static final float SELF_ROTATION_SPEED_DEGREES_PER_TICK = 0.8f;

	private static final Map<Integer, DesolationParticle> ACTIVE_PARTICLES = new HashMap<>();
	private static final int SERVER_KEEPALIVE_TIMEOUT_TICKS = 18;
	private static final int FADE_IN_TICKS = 10;
	private static final int FADE_OUT_TICKS = 12;
	private static final int ANIMATION_LENGTH_TICKS = 3;

	private static final int DUST_SPAWN_INTERVAL_TICKS = 2;
	private static final int LOW_HP_DUST_SPAWN_INTERVAL_TICKS = 1;
	private static final int DUST_PARTICLES_PER_BURST = 3;
	private static final int LOW_HP_DUST_PARTICLES_PER_BURST = 8;
	private static final int CRITICAL_HP_DUST_PARTICLES_PER_BURST = 12;
	private static final float LOW_HP_THRESHOLD = 0.30f;
	private static final float CRITICAL_HP_THRESHOLD = 0.15f;
	private static final float DUST_SIZE = 0.75f;
	private static final double DUST_Y_OFFSET = 0.045D;
	private static final double DUST_UP_SPEED = 0.012D;

	private static final DustParticleOptions DESOLATED_DUST_OPTIONS = new DustParticleOptions(
			new Vector3f(
					((DesolatedMobEffect.EFFECT_COLOR >> 16) & 255) / 255.0F,
					((DesolatedMobEffect.EFFECT_COLOR >> 8) & 255) / 255.0F,
					(DesolatedMobEffect.EFFECT_COLOR & 255) / 255.0F
			),
			DUST_SIZE
	);

	public static void clearTrackedParticles() {
		ACTIVE_PARTICLES.clear();
	}

	public static DesolationParticleProvider provider(SpriteSet spriteSet) {
		return new DesolationParticleProvider(spriteSet);
	}

	public static class DesolationParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public DesolationParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			int entityId = Mth.floor(xSpeed);

			if (entityId > 0) {
				DesolationParticle activeParticle = ACTIVE_PARTICLES.get(entityId);
				if (activeParticle != null && activeParticle.isStillAlive()) {
					activeParticle.refreshFromServerPing(x, y, z);
					return null;
				}
			}

			return new DesolationParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
		}
	}

	private final SpriteSet spriteSet;

	private final int attachedEntityId;
	private Entity attachedEntity;

	private float renderSize = 1f;
	private float lastCullingWidth = -1f;
	private float rotationDegrees = 0f;
	private float rotationDegreesOld = 0f;

	private int ticksSinceServerPing = 0;
	private int animationAge = 0;
	private int fadeInTicks = 0;
	private int fadeTicks = 0;
	private boolean fadingOut = false;

	protected DesolationParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
		super(world, x, y, z);
		this.spriteSet = spriteSet;

		this.attachedEntityId = Mth.floor(vx);

		this.lifetime = Integer.MAX_VALUE;
		this.gravity = 0f;
		this.hasPhysics = false;

		this.xd = 0;
		this.yd = 0;
		this.zd = 0;

		this.alpha = 0f;
		this.rotationDegrees = this.random.nextFloat() * 360.0f;
		this.rotationDegreesOld = this.rotationDegrees;
		this.updateCullingBounds();
		this.pickSprite(spriteSet);

		if (this.attachedEntityId > 0) {
			ACTIVE_PARTICLES.put(this.attachedEntityId, this);
		}

		Entity entity = this.getAttachedEntity();
		if (entity != null) {
			this.syncToEntity(entity, false);
		} else {
			this.setPos(x, y + Y_OFFSET, z);
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

		this.rotationDegreesOld = this.rotationDegrees;
		this.rotationDegrees = Mth.wrapDegrees(this.rotationDegrees + SELF_ROTATION_SPEED_DEGREES_PER_TICK);
		this.ticksSinceServerPing++;
		this.animationAge++;
		this.fadeInTicks = Math.min(this.fadeInTicks + 1, FADE_IN_TICKS);

		Entity entity = this.getAttachedEntity();
		if (entity == null || !entity.isAlive()) {
			this.startFadeOut();
		} else {
			this.syncToEntity(entity, true);

			if (this.shouldStayVisible(entity)) {
				this.stopFadeOut();
			} else {
				this.startFadeOut();
			}
		}

		this.setSprite(this.spriteSet.get(this.animationAge % ANIMATION_LENGTH_TICKS, ANIMATION_LENGTH_TICKS));
		this.updateAlpha();
		this.spawnDustAcrossArea();

		if (this.fadingOut && this.fadeTicks >= FADE_OUT_TICKS) {
			this.remove();
		}
	}

	private boolean shouldStayVisible(Entity entity) {
		return this.ticksSinceServerPing <= SERVER_KEEPALIVE_TIMEOUT_TICKS;
	}

	private void refreshFromServerPing(double x, double y, double z) {
		this.ticksSinceServerPing = 0;

		Entity entity = this.getAttachedEntity();
		if (entity != null) {
			this.syncToEntity(entity, false);
		} else {
			this.setPos(x, y + Y_OFFSET, z);
		}
	}

	private void startFadeOut() {
		if (!this.fadingOut) {
			this.fadingOut = true;
			this.fadeTicks = 0;
		}
	}

	private void stopFadeOut() {
		this.fadingOut = false;
		this.fadeTicks = 0;
	}

	private void updateAlpha() {
		float fadeInAlpha = Mth.clamp((float) this.fadeInTicks / (float) FADE_IN_TICKS, 0f, 1f);
		float fadeOutAlpha = 1f;

		if (this.fadingOut) {
			this.fadeTicks++;
			fadeOutAlpha = Mth.clamp(1f - ((float) this.fadeTicks / (float) FADE_OUT_TICKS), 0f, 1f);
		}

		this.alpha = fadeInAlpha * fadeOutAlpha;
	}

	private void spawnDustAcrossArea() {
		if (this.removed || this.alpha <= 0.05f)
			return;

		int spawnInterval = this.getDustSpawnIntervalTicks();
		if ((this.animationAge % spawnInterval) != 0)
			return;

		if (this.random.nextFloat() > this.alpha)
			return;

		double maxRadius = this.renderSize * 0.5D;
		int dustCount = this.getDustParticlesPerBurst();

		for (int i = 0; i < dustCount; i++) {
			double angle = this.random.nextDouble() * Math.PI * 2.0D;
			double radius = Math.sqrt(this.random.nextDouble()) * maxRadius;
			double dustX = this.x + Math.cos(angle) * radius;
			double dustZ = this.z + Math.sin(angle) * radius;

			this.level.addParticle(
					DESOLATED_DUST_OPTIONS,
					dustX,
					this.y + DUST_Y_OFFSET,
					dustZ,
					0.0D,
					DUST_UP_SPEED,
					0.0D
			);
		}
	}

	private int getDustSpawnIntervalTicks() {
		return this.isAttachedEntityLowHp() ? LOW_HP_DUST_SPAWN_INTERVAL_TICKS : DUST_SPAWN_INTERVAL_TICKS;
	}

	private int getDustParticlesPerBurst() {
		float healthPercent = this.getAttachedEntityHealthPercent();

		if (healthPercent <= CRITICAL_HP_THRESHOLD) {
			return CRITICAL_HP_DUST_PARTICLES_PER_BURST;
		}

		if (healthPercent <= LOW_HP_THRESHOLD) {
			return LOW_HP_DUST_PARTICLES_PER_BURST;
		}

		return DUST_PARTICLES_PER_BURST;
	}

	private boolean isAttachedEntityLowHp() {
		return this.getAttachedEntityHealthPercent() <= LOW_HP_THRESHOLD;
	}

	private float getAttachedEntityHealthPercent() {
		Entity entity = this.getAttachedEntity();
		if (!(entity instanceof LivingEntity livingEntity)) {
			return 1.0f;
		}

		float maxHealth = livingEntity.getMaxHealth();
		if (maxHealth <= 0.0f) {
			return 1.0f;
		}

		return Mth.clamp(livingEntity.getHealth() / maxHealth, 0.0f, 1.0f);
	}

	private boolean isStillAlive() {
		return !this.removed;
	}

	@Override
	public void remove() {
		super.remove();

		DesolationParticle activeParticle = ACTIVE_PARTICLES.get(this.attachedEntityId);
		if (activeParticle == this) {
			ACTIVE_PARTICLES.remove(this.attachedEntityId);
		}
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

	private void syncToEntity(Entity entity, boolean updateOldYaw) {
		double x = entity.getX();
		double y = entity.getY() + Y_OFFSET;
		double z = entity.getZ();

		this.setPos(x, y, z);

		float widthBasedSize = entity.getBbWidth();
		float heightBasedSize = entity.getBbHeight() * 0.45f;

		this.renderSize = Math.max(MIN_SIZE, Math.max(widthBasedSize, heightBasedSize) * SIZE_MULTIPLIER);
		this.updateCullingBounds();
	}

	private void updateCullingBounds() {
		float cullingWidth = this.renderSize * CULLING_DIAGONAL_MULTIPLIER;
		if (Math.abs(cullingWidth - this.lastCullingWidth) <= CULLING_SIZE_EPSILON)
			return;

		this.setSize(cullingWidth, CULLING_HEIGHT);
		this.lastCullingWidth = cullingWidth;
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
		Vec3 cameraPos = camera.getPosition();

		float particleX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
		float particleY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
		float particleZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

		float halfSize = this.renderSize * 0.5f;

		float selfRotation = Mth.rotLerp(partialTicks, this.rotationDegreesOld, this.rotationDegrees);
		float yawRadians = (selfRotation + ROTATION_OFFSET_DEGREES) * ((float) Math.PI / 180f);

		float cos = Mth.cos(yawRadians);
		float sin = Mth.sin(yawRadians);

		float u0 = this.getU0();
		float u1 = this.getU1();
		float v0 = this.getV0();
		float v1 = this.getV1();

		int light = this.getLightColor(partialTicks);

		this.addGroundVertex(buffer, particleX, particleY, particleZ, -halfSize, -halfSize, cos, sin, u0, v0, light);
		this.addGroundVertex(buffer, particleX, particleY, particleZ, -halfSize, halfSize, cos, sin, u0, v1, light);
		this.addGroundVertex(buffer, particleX, particleY, particleZ, halfSize, halfSize, cos, sin, u1, v1, light);
		this.addGroundVertex(buffer, particleX, particleY, particleZ, halfSize, -halfSize, cos, sin, u1, v0, light);
	}

	private void addGroundVertex(VertexConsumer buffer, float centerX, float centerY, float centerZ, float localX, float localZ, float cos, float sin, float u, float v, int light) {
		float rotatedX = localX * cos - localZ * sin;
		float rotatedZ = localX * sin + localZ * cos;

		buffer.addVertex(centerX + rotatedX, centerY, centerZ + rotatedZ)
				.setUv(u, v)
				.setColor(this.rCol, this.gCol, this.bCol, this.alpha)
				.setLight(light)
				.setNormal(0f, 1f, 0f);
	}

	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}
}
