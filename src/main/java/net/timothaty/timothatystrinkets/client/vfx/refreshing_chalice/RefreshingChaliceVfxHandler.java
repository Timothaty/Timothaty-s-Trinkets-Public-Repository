package net.timothaty.timothatystrinkets.client.vfx.refreshing_chalice;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class RefreshingChaliceVfxHandler {
	private static final int LIFETIME_TICKS = 40;
	private static final float DOT_RED = 0xE0 / 255.0F;
	private static final float DOT_GREEN = 0x1D / 255.0F;
	private static final float DOT_BLUE = 0x16 / 255.0F;
	private static final List<Effect> EFFECTS = new ArrayList<>();

	private RefreshingChaliceVfxHandler() {
	}

	public static void spawn(int entityId) {
		EFFECTS.removeIf(effect -> effect.entityId() == entityId);
		EFFECTS.add(new Effect(entityId));
	}

	public static List<Effect> effects() {
		return EFFECTS;
	}

	public static void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			EFFECTS.clear();
			return;
		}

		for (int i = EFFECTS.size() - 1; i >= 0; i--) {
			Effect effect = EFFECTS.get(i);
			if (!effect.tick()) {
				EFFECTS.remove(i);
				continue;
			}

			Entity entity = level.getEntity(effect.entityId());
			if (entity instanceof LivingEntity living && living.isAlive()) {
				spawnDots(minecraft, level, living, effect);
			}
		}
	}

	private static void spawnDots(Minecraft minecraft, ClientLevel level, LivingEntity living, Effect effect) {
		ParticleStatus particleStatus = minecraft.options.particles().get();
		if (particleStatus == ParticleStatus.MINIMAL)
			return;
		if (particleStatus == ParticleStatus.DECREASED && effect.age() % 2 != 0)
			return;

		float fade = effect.fade(0.0F);
		if (fade <= 0.05F)
			return;

		RandomSource random = level.random;
		int count = particleStatus == ParticleStatus.DECREASED ? 3 : 7;
		double bodyRadius = Math.max(0.24D, living.getBbWidth() * 0.48D);
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double radius = bodyRadius * (0.45D + random.nextDouble() * 0.85D);
			double x = living.getX() + Math.cos(angle) * radius;
			double y = living.getY() + 0.18D + random.nextDouble() * Math.max(0.85D, living.getBbHeight() * 0.78D);
			double z = living.getZ() + Math.sin(angle) * radius;
			level.addParticle(TimothatysTrinketsModParticleTypes.DOT.get(), x, y, z, DOT_RED, DOT_GREEN, DOT_BLUE);
		}
	}

	public static final class Effect {
		private final int entityId;
		private final float phase;
		private int age;

		private Effect(int entityId) {
			this.entityId = entityId;
			this.phase = (float) (Math.random() * Math.PI * 2.0D);
		}

		private boolean tick() {
			age++;
			return age < LIFETIME_TICKS;
		}

		public int entityId() {
			return entityId;
		}

		public int age() {
			return age;
		}

		public float phase() {
			return phase;
		}

		public float progress(float partialTick) {
			return Mth.clamp((age + partialTick) / (float) LIFETIME_TICKS, 0.0F, 1.0F);
		}

		public float fade(float partialTick) {
			float progress = progress(partialTick);
			float fadeIn = smoothstep(0.0F, 0.16F, progress);
			float fadeOut = 1.0F - smoothstep(0.72F, 1.0F, progress);
			float fade = fadeIn * fadeOut;
			return fade <= 0.004F ? 0.0F : fade;
		}
	}

	static float smoothstep(float edge0, float edge1, float x) {
		if (edge0 == edge1)
			return x < edge0 ? 0.0F : 1.0F;

		x = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}
}
