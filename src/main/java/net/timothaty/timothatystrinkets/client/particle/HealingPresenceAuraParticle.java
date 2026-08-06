package net.timothaty.timothatystrinkets.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
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

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class HealingPresenceAuraParticle extends TextureSheetParticle {
	private static final float RENDER_SIZE = 2.0F;
	private static final float Y_OFFSET = 0.025F;
	private static final float ROTATION_SPEED_DEGREES_PER_TICK = 0.45F;
	private static final int SERVER_KEEPALIVE_TIMEOUT_TICKS = 18;

	private static final Map<Integer, HealingPresenceAuraParticle> ACTIVE_PARTICLES = new HashMap<>();

	private final int attachedEntityId;
	private Entity attachedEntity;
	private float rotationDegrees;
	private float rotationDegreesOld;
	private int ticksSinceServerPing;

	private HealingPresenceAuraParticle(
			ClientLevel level,
			double x,
			double y,
			double z,
			double entityId,
			SpriteSet spriteSet
	) {
		super(level, x, y, z);
		this.attachedEntityId = Mth.floor(entityId);
		this.lifetime = Integer.MAX_VALUE;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.alpha = 1.0F;
		this.rotationDegrees = this.random.nextFloat() * 360.0F;
		this.rotationDegreesOld = this.rotationDegrees;
		this.setSize(0.01F, 0.01F);
		this.pickSprite(spriteSet);

		if (this.attachedEntityId > 0)
			ACTIVE_PARTICLES.put(this.attachedEntityId, this);

		Entity entity = getAttachedEntity();
		if (entity != null) {
			syncToEntity(entity);
		} else {
			setPos(x, y + Y_OFFSET, z);
		}
	}

	public static ParticleProvider<SimpleParticleType> provider(SpriteSet spriteSet) {
		return (type, level, x, y, z, xSpeed, ySpeed, zSpeed) -> {
			int entityId = Mth.floor(xSpeed);
			if (entityId > 0) {
				HealingPresenceAuraParticle active = ACTIVE_PARTICLES.get(entityId);
				if (active != null && !active.removed) {
					active.refreshFromServerPing(x, y, z);
					return null;
				}
			}

			return new HealingPresenceAuraParticle(level, x, y, z, xSpeed, spriteSet);
		};
	}

	public static void clearTrackedParticles() {
		for (HealingPresenceAuraParticle particle : List.copyOf(ACTIVE_PARTICLES.values()))
			particle.remove();
		ACTIVE_PARTICLES.clear();
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
		this.rotationDegrees = Mth.wrapDegrees(this.rotationDegrees + ROTATION_SPEED_DEGREES_PER_TICK);
		this.ticksSinceServerPing++;

		Entity entity = getAttachedEntity();
		if (entity == null
				|| !entity.isAlive()
				|| entity.isRemoved()
				|| this.ticksSinceServerPing > SERVER_KEEPALIVE_TIMEOUT_TICKS) {
			remove();
			return;
		}

		syncToEntity(entity);
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
		if (isHiddenForFirstPersonLocalPlayer())
			return;

		Vec3 cameraPos = camera.getPosition();
		float centerX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
		float centerY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
		float centerZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());
		float halfSize = RENDER_SIZE * 0.5F;
		float rotationRadians = Mth.rotLerp(partialTicks, this.rotationDegreesOld, this.rotationDegrees)
				* ((float) Math.PI / 180.0F);
		float cos = Mth.cos(rotationRadians);
		float sin = Mth.sin(rotationRadians);
		int light = getLightColor(partialTicks);

		addGroundVertex(buffer, centerX, centerY, centerZ, -halfSize, -halfSize, cos, sin, getU0(), getV0(), light);
		addGroundVertex(buffer, centerX, centerY, centerZ, -halfSize, halfSize, cos, sin, getU0(), getV1(), light);
		addGroundVertex(buffer, centerX, centerY, centerZ, halfSize, halfSize, cos, sin, getU1(), getV1(), light);
		addGroundVertex(buffer, centerX, centerY, centerZ, halfSize, -halfSize, cos, sin, getU1(), getV0(), light);
	}

	@Override
	public int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	@Override
	public void remove() {
		super.remove();
		HealingPresenceAuraParticle active = ACTIVE_PARTICLES.get(this.attachedEntityId);
		if (active == this)
			ACTIVE_PARTICLES.remove(this.attachedEntityId);
	}

	private void refreshFromServerPing(double x, double y, double z) {
		this.ticksSinceServerPing = 0;
		Entity entity = getAttachedEntity();
		if (entity != null) {
			syncToEntity(entity);
		} else {
			setPos(x, y + Y_OFFSET, z);
		}
	}

	private Entity getAttachedEntity() {
		if (this.attachedEntity != null
				&& this.attachedEntity.level() == this.level
				&& this.attachedEntity.isAlive()
				&& !this.attachedEntity.isRemoved())
			return this.attachedEntity;

		this.attachedEntity = this.attachedEntityId > 0 ? this.level.getEntity(this.attachedEntityId) : null;
		return this.attachedEntity;
	}

	private void syncToEntity(Entity entity) {
		setPos(entity.getX(), entity.getY() + Y_OFFSET, entity.getZ());
	}

	private boolean isHiddenForFirstPersonLocalPlayer() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null
				&& getAttachedEntity() == minecraft.player
				&& minecraft.options.getCameraType() == CameraType.FIRST_PERSON;
	}

	private void addGroundVertex(
			VertexConsumer buffer,
			float centerX,
			float centerY,
			float centerZ,
			float localX,
			float localZ,
			float cos,
			float sin,
			float u,
			float v,
			int light
	) {
		float rotatedX = localX * cos - localZ * sin;
		float rotatedZ = localX * sin + localZ * cos;
		buffer.addVertex(centerX + rotatedX, centerY, centerZ + rotatedZ)
				.setUv(u, v)
				.setColor(this.rCol, this.gCol, this.bCol, 1.0F)
				.setLight(light)
				.setNormal(0.0F, 1.0F, 0.0F);
	}
}
