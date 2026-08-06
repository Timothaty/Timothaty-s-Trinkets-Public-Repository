package net.timothaty.timothatystrinkets.client.beatific_pallium;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.BeatificPalliumEntity;
import net.timothaty.timothatystrinkets.network.BeatificPalliumImpactMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class BeatificPalliumClientState {
	private static final int MAX_RIPPLES = 4;
	private static final int RUNE_PULSE_TICKS = 6;
	private static final Map<Integer, VisualState> STATES = new HashMap<>();

	private BeatificPalliumClientState() {
	}

	public static void addImpact(BeatificPalliumImpactMessage message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || message == null)
			return;

		long gameTime = minecraft.level.getGameTime();
		VisualState state = STATES.computeIfAbsent(message.palliumEntityId(), ignored -> new VisualState());
		state.addRipple(new Ripple(
				Mth.clamp(message.face(), 0, 5),
				Mth.clamp(message.u(), 0.0F, 1.0F),
				Mth.clamp(message.v(), 0.0F, 1.0F),
				Mth.clamp(message.absorbedDamage(), 0.0F, 30.0F),
				message.seed(),
				gameTime,
				10 + (message.seed() & 1)
		));

		float currentPulse = state.runePulse(gameTime);
		float newStrength = Mth.clamp(0.25F + message.absorbedDamage() / 10.0F, 0.25F, 1.0F);
		state.runePulseStrength = Math.max(currentPulse, newStrength);
		state.runePulseStartGameTime = gameTime;
	}

	public static RenderView renderView(BeatificPalliumEntity entity, float partialTick) {
		if (entity == null)
			return RenderView.EMPTY;
		VisualState state = STATES.get(entity.getId());
		if (state == null)
			return RenderView.EMPTY;

		if (entity.getVisualPhase() == BeatificPalliumEntity.VisualPhase.BURST
				|| entity.getVisualPhase() == BeatificPalliumEntity.VisualPhase.FADING) {
			STATES.remove(entity.getId());
			return RenderView.EMPTY;
		}

		double now = entity.level().getGameTime() + partialTick;
		state.removeExpiredRipples(now);
		state.renderNow = now;
		state.renderRunePulse = state.runePulse(now);
		if (state.rippleCount == 0 && state.renderRunePulse <= 0.0F) {
			STATES.remove(entity.getId());
			return RenderView.EMPTY;
		}
		return state.renderView;
	}

	public static void clearForPallium(int palliumEntityId) {
		STATES.remove(palliumEntityId);
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
		if (event.getLevel().isClientSide() && event.getEntity() instanceof BeatificPalliumEntity)
			clearForPallium(event.getEntity().getId());
	}

	public record Ripple(int face, float u, float v, float absorbedDamage, int seed, long startGameTime, int lifetimeTicks) {
	}

	public static final class RenderView {
		private static final RenderView EMPTY = new RenderView(null);
		private final VisualState state;

		private RenderView(VisualState state) {
			this.state = state;
		}

		public int rippleCount() {
			return state == null ? 0 : state.rippleCount;
		}

		public Ripple rippleAt(int index) {
			if (state == null || index < 0 || index >= state.rippleCount)
				return null;
			return state.rippleAt(index);
		}

		public float runePulse() {
			return state == null ? 0.0F : state.renderRunePulse;
		}

		public double now() {
			return state == null ? 0.0D : state.renderNow;
		}
	}

	private static final class VisualState {
		private final Ripple[] ripples = new Ripple[MAX_RIPPLES];
		private final RenderView renderView = new RenderView(this);
		private int rippleStart;
		private int rippleCount;
		private float runePulseStrength;
		private long runePulseStartGameTime;
		private float renderRunePulse;
		private double renderNow;

		private void addRipple(Ripple ripple) {
			if (rippleCount < MAX_RIPPLES) {
				ripples[(rippleStart + rippleCount) % MAX_RIPPLES] = ripple;
				rippleCount++;
				return;
			}
			ripples[rippleStart] = ripple;
			rippleStart = (rippleStart + 1) % MAX_RIPPLES;
		}

		private Ripple rippleAt(int index) {
			return ripples[(rippleStart + index) % MAX_RIPPLES];
		}

		private void removeExpiredRipples(double now) {
			for (int index = rippleCount - 1; index >= 0; index--) {
				Ripple ripple = rippleAt(index);
				if (ripple != null && now - ripple.startGameTime() >= ripple.lifetimeTicks())
					removeRippleAt(index);
			}
		}

		private void removeRippleAt(int index) {
			for (int shifted = index; shifted < rippleCount - 1; shifted++) {
				ripples[(rippleStart + shifted) % MAX_RIPPLES] = rippleAt(shifted + 1);
			}
			ripples[(rippleStart + rippleCount - 1) % MAX_RIPPLES] = null;
			rippleCount--;
			if (rippleCount == 0)
				rippleStart = 0;
		}

		private float runePulse(double now) {
			double elapsed = Math.max(0.0D, now - this.runePulseStartGameTime);
			return elapsed >= RUNE_PULSE_TICKS
					? 0.0F
					: this.runePulseStrength * (1.0F - (float) (elapsed / RUNE_PULSE_TICKS));
		}
	}
}
