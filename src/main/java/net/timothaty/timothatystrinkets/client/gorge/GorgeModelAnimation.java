package net.timothaty.timothatystrinkets.client.gorge;

import net.timothaty.timothatystrinkets.client.model.animations.GorgeAnimation;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GorgeModelAnimation {
	private static final float WAIST_PIVOT_Y = 12.0F;
	private static final Vector3f ANIMATION_VECTOR_CACHE = new Vector3f();

	private GorgeModelAnimation() {
	}

	public static void apply(
			float elapsedTicks,
			ModelPart head,
			ModelPart body,
			ModelPart rightArm,
			ModelPart leftArm
	) {
		AnimationDefinition animation =
				GorgeAnimation.GORGE_CONSUMPTION;
		float normalizedProgress = Mth.clamp(
				elapsedTicks / GorgeAnimationState.DURATION_TICKS,
				0.0F,
				1.0F
		);
		float elapsedSeconds =
				normalizedProgress * animation.lengthInSeconds();
		Vector3f waistRotation = new Vector3f();
		Vector3f waistPosition = new Vector3f();
		Vector3f waistScaleOffset = new Vector3f();

		for (Map.Entry<String, List<AnimationChannel>> entry
				: animation.boneAnimations().entrySet()) {
			String boneName = normalizeBoneName(entry.getKey());
			if ("waist".equals(boneName)) {
				sampleVirtualWaist(
						entry.getValue(),
						elapsedSeconds,
						waistRotation,
						waistPosition,
						waistScaleOffset
				);
				continue;
			}
			ModelPart part = findPart(
					boneName,
					head,
					body,
					rightArm,
					leftArm
			);
			if (part == null)
				continue;
			for (AnimationChannel channel : entry.getValue()) {
				applyChannel(part, channel, elapsedSeconds);
			}
		}

		applyVirtualWaist(
				waistRotation,
				waistPosition,
				waistScaleOffset,
				body,
				head,
				rightArm,
				leftArm
		);
	}

	private static void sampleVirtualWaist(
			List<AnimationChannel> channels,
			float elapsedSeconds,
			Vector3f rotation,
			Vector3f position,
			Vector3f scaleOffset
	) {
		for (AnimationChannel channel : channels) {
			Vector3f value = sampleChannel(channel, elapsedSeconds);
			if (channel.target() == AnimationChannel.Targets.ROTATION) {
				rotation.add(value);
			} else if (channel.target()
					== AnimationChannel.Targets.POSITION) {
				position.add(value);
			} else if (channel.target()
					== AnimationChannel.Targets.SCALE) {
				scaleOffset.add(value);
			}
		}
	}

	private static void applyChannel(
			ModelPart part,
			AnimationChannel channel,
			float elapsedSeconds
	) {
		channel.target().apply(
				part,
				sampleChannel(channel, elapsedSeconds)
		);
	}

	private static Vector3f sampleChannel(
			AnimationChannel channel,
			float elapsedSeconds
	) {
		Keyframe[] keyframes = channel.keyframes();
		int currentIndex = Math.max(
				0,
				Mth.binarySearch(
						0,
						keyframes.length,
						index -> elapsedSeconds <= keyframes[index].timestamp()
				) - 1
		);
		int nextIndex = Math.min(
				keyframes.length - 1,
				currentIndex + 1
		);
		Keyframe current = keyframes[currentIndex];
		Keyframe next = keyframes[nextIndex];
		float keyframeProgress = nextIndex == currentIndex
				? 0.0F
				: Mth.clamp(
						(elapsedSeconds - current.timestamp())
								/ (next.timestamp() - current.timestamp()),
						0.0F,
						1.0F
				);
		next.interpolation().apply(
				ANIMATION_VECTOR_CACHE,
				keyframeProgress,
				keyframes,
				currentIndex,
				nextIndex,
				1.0F
		);
		return ANIMATION_VECTOR_CACHE;
	}

	private static void applyVirtualWaist(
			Vector3f rotation,
			Vector3f position,
			Vector3f scaleOffset,
			ModelPart... upperBodyParts
	) {
		float scaleX = 1.0F + scaleOffset.x();
		float scaleY = 1.0F + scaleOffset.y();
		float scaleZ = 1.0F + scaleOffset.z();
		Quaternionf parentRotation = new Quaternionf().rotationZYX(
				rotation.z(),
				rotation.y(),
				rotation.x()
		);

		for (ModelPart part : upperBodyParts) {
			Vector3f relativePivot = new Vector3f(
					part.x,
					part.y - WAIST_PIVOT_Y,
					part.z
			);
			relativePivot.mul(scaleX, scaleY, scaleZ);
			parentRotation.transform(relativePivot);
			part.x = relativePivot.x() + position.x();
			part.y = relativePivot.y()
					+ WAIST_PIVOT_Y
					+ position.y();
			part.z = relativePivot.z() + position.z();

			Quaternionf localRotation = new Quaternionf().rotationZYX(
					part.zRot,
					part.yRot,
					part.xRot
			);
			Vector3f combinedEuler = new Quaternionf(parentRotation)
					.mul(localRotation)
					.getEulerAnglesZYX(new Vector3f());
			part.xRot = combinedEuler.x();
			part.yRot = combinedEuler.y();
			part.zRot = combinedEuler.z();
			part.xScale *= scaleX;
			part.yScale *= scaleY;
			part.zScale *= scaleZ;
		}
	}

	private static ModelPart findPart(
			String boneName,
			ModelPart head,
			ModelPart body,
			ModelPart rightArm,
			ModelPart leftArm
	) {
		return switch (boneName) {
			case "head" -> head;
			case "body" -> body;
			case "rightarm" -> rightArm;
			case "leftarm" -> leftArm;
			default -> null;
		};
	}

	private static String normalizeBoneName(String boneName) {
		return boneName.replace(" ", "")
				.replace("_", "")
				.toLowerCase(Locale.ROOT);
	}
}
