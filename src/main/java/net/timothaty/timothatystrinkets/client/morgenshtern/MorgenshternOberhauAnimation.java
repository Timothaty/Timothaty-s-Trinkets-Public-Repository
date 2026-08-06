package net.timothaty.timothatystrinkets.client.morgenshtern;

import net.minecraft.util.Mth;

public final class MorgenshternOberhauAnimation {
	public static final float DURATION_TICKS = 5.0F;
	private static final float MIDDLE_KEYFRAME_TICKS = 1.666F;

	private static final Pose START = new Pose(-155.0F, 0.0F, 0.0F);
	private static final Pose MIDDLE =
			new Pose(-77.3518F, -2.6099F, 1.7863F);
	private static final Pose END = new Pose(0.0F, 0.0F, 0.0F);

	private MorgenshternOberhauAnimation() {
	}

	public static Pose sample(float elapsedTicks) {
		float time = Mth.clamp(elapsedTicks, 0.0F, DURATION_TICKS);
		if (time <= MIDDLE_KEYFRAME_TICKS) {
			float delta = time / MIDDLE_KEYFRAME_TICKS;
			return catmullRom(delta, START, START, MIDDLE, END);
		}

		float delta = (time - MIDDLE_KEYFRAME_TICKS)
				/ (DURATION_TICKS - MIDDLE_KEYFRAME_TICKS);
		return catmullRom(delta, START, MIDDLE, END, END);
	}

	private static Pose catmullRom(
			float delta,
			Pose point1,
			Pose point2,
			Pose point3,
			Pose point4
	) {
		return new Pose(
				Mth.catmullrom(
						delta,
						point1.xDegrees(),
						point2.xDegrees(),
						point3.xDegrees(),
						point4.xDegrees()
				),
				Mth.catmullrom(
						delta,
						point1.yDegrees(),
						point2.yDegrees(),
						point3.yDegrees(),
						point4.yDegrees()
				),
				Mth.catmullrom(
						delta,
						point1.zDegrees(),
						point2.zDegrees(),
						point3.zDegrees(),
						point4.zDegrees()
				)
		);
	}

	public record Pose(
			float xDegrees,
			float yDegrees,
			float zDegrees
	) {
	}
}
