package net.timothaty.timothatystrinkets.client.vfx.soul_rip;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SoulRipTrail {
	private static final int POINTS = 8;
	private static final float FULL_TURN = 6.2831855F;

	private final Vec3 start;
	private final Vec3 control;
	private final Vec3 end;
	private final int lifetime;
	private final float width;
	private final float alpha;
	private final float phase;
	private final float turnRate;
	private final double curveRadius;
	private int age;

	public SoulRipTrail(Vec3 start, Vec3 control, Vec3 end, int lifetime, float width, float alpha, float phase, float turnRate, double curveRadius) {
		this.start = start;
		this.control = control;
		this.end = end;
		this.lifetime = Math.max(1, lifetime);
		this.width = width;
		this.alpha = alpha;
		this.phase = phase;
		this.turnRate = turnRate;
		this.curveRadius = curveRadius;
	}

	public boolean tick() {
		age++;
		return age < lifetime;
	}

	public int pointCount() {
		return POINTS;
	}

	public Vec3 pointAt(float t) {
		float clamped = Mth.clamp(t, 0.0F, 1.0F);
		Vec3 base = bezier(start, control, end, clamped);
		float angle = phase + age * turnRate + clamped * FULL_TURN * 0.72F;
		float middle = (float) Math.sin(Math.PI * clamped);
		double radius = curveRadius * middle * (0.25D + 0.75D * visibleProgress());
		return base.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
	}

	public float visibleProgress() {
		float progress = age / (float) lifetime;
		float grow = Mth.clamp(progress / 0.42F, 0.0F, 1.0F);
		return easeOutCubic(grow);
	}

	public float width() {
		float progress = age / (float) lifetime;
		float fade = Mth.clamp((1.0F - progress) / 0.34F, 0.0F, 1.0F);
		float grow = Mth.clamp(progress / 0.18F, 0.0F, 1.0F);
		return width * fade * easeOutCubic(grow);
	}

	public float alpha() {
		float progress = age / (float) lifetime;
		float fadeIn = Mth.clamp(progress / 0.12F, 0.0F, 1.0F);
		float fadeOut = Mth.clamp((1.0F - progress) / 0.42F, 0.0F, 1.0F);
		return alpha * easeOutCubic(fadeIn) * fadeOut;
	}

	private static float easeOutCubic(float value) {
		float inverse = 1.0F - Mth.clamp(value, 0.0F, 1.0F);
		return 1.0F - inverse * inverse * inverse;
	}

	private static Vec3 bezier(Vec3 start, Vec3 control, Vec3 end, float t) {
		double u = 1.0D - t;
		double tt = t * t;
		double uu = u * u;
		return start.scale(uu).add(control.scale(2.0D * u * t)).add(end.scale(tt));
	}
}
