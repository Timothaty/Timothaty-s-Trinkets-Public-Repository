package net.timothaty.timothatystrinkets.client.particle;

import org.joml.Vector3f;

import net.timothaty.timothatystrinkets.util.VoidMarkParticleData;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class VoidMarkParticle extends TextureSheetParticle {
	private static final int FADE_IN_TICKS = 5;
	private static final int FADE_OUT_TICKS = 8;
	private static final int REFRESH_GRACE_TICKS = 8;
	private static final int MAX_LIFETIME_TICKS = 20 * 60;
	private static final float BASE_QUAD_SIZE = 0.62F;
	private static final double HEIGHT_OFFSET_ABOVE_HEAD = 0.82D;

	private static final DustParticleOptions VOID_LIGHT_DUST = new DustParticleOptions(new Vector3f(0.722F, 0.302F, 1.0F), 0.45F);
	private static final Map<Integer, VoidMarkParticle> ACTIVE_PARTICLES = new HashMap<>();

	public static void clearTrackedParticles() {
		ACTIVE_PARTICLES.clear();
	}

	private final int targetEntityId;
	private LivingEntity target;
	private int fadeInAge = 0;
	private int fadeOutAge = 0;
	private int lastRefreshAge = 0;
	private boolean fadingOut = false;

	public static VoidMarkParticleProvider provider(SpriteSet spriteSet) {
		return new VoidMarkParticleProvider(spriteSet);
	}

	public static class VoidMarkParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public VoidMarkParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			int entityId = VoidMarkParticleData.decodeEntityId(xSpeed, ySpeed);
			if (entityId < 0) {
				return null;
			}

			cleanupActiveParticles();

			Entity entity = worldIn.getEntity(entityId);
			if (!(entity instanceof LivingEntity livingEntity))
				return null;

			VoidMarkParticle active = ACTIVE_PARTICLES.get(entityId);
			if (active != null && !active.removed) {
				active.refresh(livingEntity);
				return null;
			}

			VoidMarkParticle particle = new VoidMarkParticle(worldIn, livingEntity, spriteSet);
			ACTIVE_PARTICLES.put(entityId, particle);
			return particle;
		}
	}

	private VoidMarkParticle(ClientLevel level, LivingEntity target, SpriteSet spriteSet) {
		super(level, target.getX(), getMarkY(target), target.getZ());
		this.target = target;
		this.targetEntityId = target.getId();
		this.setSize(0.01F, 0.01F);
		this.lifetime = MAX_LIFETIME_TICKS;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.xd = 0.0D;
		this.yd = 0.0D;
		this.zd = 0.0D;
		this.quadSize = BASE_QUAD_SIZE;
		this.setAlpha(0.0F);
		this.pickSprite(spriteSet);
		this.refresh(target);
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

		if (++this.age >= this.lifetime) {
			this.remove();
			return;
		}

		Entity entity = this.level.getEntity(this.targetEntityId);
		if (entity instanceof LivingEntity livingEntity) {
			this.target = livingEntity;
		}

		if (target == null || !target.isAlive() || target.isRemoved()) {
			startFadeOut();
		} else if (this.age - this.lastRefreshAge > REFRESH_GRACE_TICKS) {
			startFadeOut();
		} else if (fadingOut) {
			fadingOut = false;
			fadeOutAge = 0;
		}

		updatePosition();
		if (!updateAlphaAndScale())
			return;
		spawnRareDust(!fadingOut);
	}

	@Override
	public void remove() {
		super.remove();
		VoidMarkParticle active = ACTIVE_PARTICLES.get(this.targetEntityId);
		if (active == this) {
			ACTIVE_PARTICLES.remove(this.targetEntityId);
		}
	}

	private void refresh(LivingEntity target) {
		this.target = target;
		this.lastRefreshAge = this.age;
		if (this.fadingOut) {
			this.fadingOut = false;
			this.fadeOutAge = 0;
		}
	}

	private void updatePosition() {
		if (target == null)
			return;

		double bob = Math.sin((this.age + this.random.nextFloat()) * 0.16F) * 0.025D;
		this.setPos(target.getX(), getMarkY(target) + bob, target.getZ());
	}

	private boolean updateAlphaAndScale() {
		float alpha;

		if (fadingOut) {
			fadeOutAge++;
			alpha = clamp01(1.0F - fadeOutAge / (float) FADE_OUT_TICKS);
			if (fadeOutAge >= FADE_OUT_TICKS) {
				this.remove();
				return false;
			}
		} else {
			fadeInAge++;
			alpha = clamp01(fadeInAge / (float) FADE_IN_TICKS);
		}

		float pulse = 0.96F + 0.04F * (float) Math.sin(this.age * 0.22F);
		this.quadSize = BASE_QUAD_SIZE * pulse;
		this.setAlpha(alpha);
		return true;
	}

	private void startFadeOut() {
		if (!fadingOut) {
			fadingOut = true;
			fadeOutAge = 0;
		}
	}

	private void spawnRareDust(boolean canSpawn) {
		if (!canSpawn)
			return;
		if (this.random.nextInt(20) != 0)
			return;

		double xOffset = (this.random.nextDouble() - 0.5D) * 0.18D;
		double yOffset = (this.random.nextDouble() - 0.5D) * 0.08D;
		double zOffset = (this.random.nextDouble() - 0.5D) * 0.18D;

		this.level.addParticle(VOID_LIGHT_DUST, this.x + xOffset, this.y + yOffset, this.z + zOffset, 0.0D, 0.004D, 0.0D);
	}

	private static void cleanupActiveParticles() {
		Iterator<Map.Entry<Integer, VoidMarkParticle>> iterator = ACTIVE_PARTICLES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, VoidMarkParticle> entry = iterator.next();
			if (entry.getValue() == null || entry.getValue().removed) {
				iterator.remove();
			}
		}
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private static double getMarkY(LivingEntity entity) {
		return entity.getY() + entity.getBbHeight() + HEIGHT_OFFSET_ABOVE_HEAD;
	}
}
