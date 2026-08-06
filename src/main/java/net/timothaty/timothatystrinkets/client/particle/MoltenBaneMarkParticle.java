package net.timothaty.timothatystrinkets.client.particle;

import org.joml.Vector3f;

import net.timothaty.timothatystrinkets.client.MoltenBaneOverlayClient;
import net.timothaty.timothatystrinkets.util.MoltenBaneMarkParticleData;

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
public class MoltenBaneMarkParticle extends TextureSheetParticle {
	private static final int FADE_IN_TICKS = 4;
	private static final int FADE_OUT_TICKS = 8;
	private static final int BURST_FADE_TICKS = 5;
	private static final int DISPLAY_TICKS = 20 * 3;
	private static final int OVERLAY_VISUAL_TICKS = 8;
	private static final int MAX_LIFETIME_TICKS = 20 * 60;
	private static final float BASE_QUAD_SIZE = 0.52F;
	private static final float BURST_QUAD_SIZE = 0.82F;
	private static final double HEIGHT_OFFSET_ABOVE_HEAD = 0.72D;

	private static final DustParticleOptions EMBER_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.34F, 0.02F), 0.6F);
	private static final Map<Integer, MoltenBaneMarkParticle> ACTIVE_PARTICLES = new HashMap<>();

	public static void clearTrackedParticles() {
		ACTIVE_PARTICLES.clear();
	}

	private final SpriteSet spriteSet;
	private final int targetEntityId;
	private LivingEntity target;
	private int stage;
	private int fadeInAge = 0;
	private int fadeOutAge = 0;
	private int burstAge = 0;
	private int displayAge = 0;
	private boolean fadingOut = false;
	private boolean bursting = false;

	public static MoltenBaneMarkParticleProvider provider(SpriteSet spriteSet) {
		return new MoltenBaneMarkParticleProvider(spriteSet);
	}

	public static class MoltenBaneMarkParticleProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public MoltenBaneMarkParticleProvider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Override
		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			int entityId = MoltenBaneMarkParticleData.decodeEntityId(xSpeed, ySpeed);
			if (entityId < 0)
				return null;

			int stage = MoltenBaneMarkParticleData.decodeStage(zSpeed);
			if (stage == MoltenBaneMarkParticleData.VISUAL_OVERLAY_STAGE) {
				MoltenBaneOverlayClient.activateVisual(entityId, OVERLAY_VISUAL_TICKS);
				return null;
			}

			cleanupActiveParticles();

			Entity entity = worldIn.getEntity(entityId);
			if (!(entity instanceof LivingEntity livingEntity))
				return null;

			MoltenBaneMarkParticle active = ACTIVE_PARTICLES.get(entityId);
			if (active != null && !active.removed) {
				active.refresh(livingEntity, stage);
				return null;
			}

			MoltenBaneMarkParticle particle = new MoltenBaneMarkParticle(worldIn, livingEntity, stage, this.spriteSet);
			ACTIVE_PARTICLES.put(entityId, particle);
			return particle;
		}
	}

	private MoltenBaneMarkParticle(ClientLevel level, LivingEntity target, int stage, SpriteSet spriteSet) {
		super(level, target.getX(), getMarkY(target), target.getZ());
		this.spriteSet = spriteSet;
		this.target = target;
		this.targetEntityId = target.getId();
		this.stage = stage;
		this.setSize(0.01F, 0.01F);
		this.lifetime = MAX_LIFETIME_TICKS;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.xd = 0.0D;
		this.yd = 0.0D;
		this.zd = 0.0D;
		this.quadSize = BASE_QUAD_SIZE;
		this.setAlpha(0.0F);
		this.setSpriteForStage();
		this.refresh(target, stage);
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
		} else if (!bursting && !fadingOut && ++this.displayAge >= DISPLAY_TICKS) {
			startFadeOut();
		}

		updatePosition();
		if (!updateAlphaAndScale())
			return;
		spawnRareEmbers(!fadingOut);
	}

	@Override
	public void remove() {
		super.remove();
		MoltenBaneMarkParticle active = ACTIVE_PARTICLES.get(this.targetEntityId);
		if (active == this) {
			ACTIVE_PARTICLES.remove(this.targetEntityId);
		}
	}

	private void refresh(LivingEntity target, int stage) {
		this.target = target;
		this.displayAge = 0;

		if (this.stage != stage) {
			this.stage = stage;
			this.fadeInAge = 0;
			this.setSpriteForStage();
		}

		if (stage >= 4) {
			this.bursting = true;
			this.fadingOut = false;
			this.burstAge = 0;
			this.fadeOutAge = 0;
			return;
		}

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
		if (bursting) {
			burstAge++;
			float progress = clamp01(burstAge / (float) BURST_FADE_TICKS);
			this.quadSize = BASE_QUAD_SIZE + (BURST_QUAD_SIZE - BASE_QUAD_SIZE) * progress;
			this.setAlpha(1.0F - progress);
			if (burstAge >= BURST_FADE_TICKS) {
				this.remove();
				return false;
			}
			return true;
		}

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

		float pulse = 0.96F + 0.04F * (float) Math.sin(this.age * 0.28F);
		this.quadSize = BASE_QUAD_SIZE * pulse;
		this.setAlpha(alpha);
		return true;
	}

	private void setSpriteForStage() {
		this.setSprite(this.spriteSet.get(Math.max(0, this.stage - 1), 3));
	}

	private void startFadeOut() {
		if (!fadingOut) {
			fadingOut = true;
			fadeOutAge = 0;
		}
	}

	private void spawnRareEmbers(boolean canSpawn) {
		if (!canSpawn)
			return;
		if (this.random.nextInt(bursting ? 2 : 12) != 0)
			return;

		double xOffset = (this.random.nextDouble() - 0.5D) * 0.22D;
		double yOffset = (this.random.nextDouble() - 0.5D) * 0.1D;
		double zOffset = (this.random.nextDouble() - 0.5D) * 0.22D;
		this.level.addParticle(EMBER_DUST, this.x + xOffset, this.y + yOffset, this.z + zOffset, 0.0D, 0.006D, 0.0D);
	}

	private static void cleanupActiveParticles() {
		Iterator<Map.Entry<Integer, MoltenBaneMarkParticle>> iterator = ACTIVE_PARTICLES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, MoltenBaneMarkParticle> entry = iterator.next();
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
