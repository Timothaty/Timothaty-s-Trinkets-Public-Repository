package net.timothaty.timothatystrinkets.client.hubris;

import net.timothaty.timothatystrinkets.client.model.animations.HubrisActivation;
import net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium.hubris.HubrisAnimationVariant;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HubrisModelAnimation {
	private static final AnimationDefinition MIRRORED_HEAVY = mirrorHeavyAnimation(
			HubrisActivation.HUBRIS_ACTIVATION_MACE
	);
	private static final Vector3f ANIMATION_VECTOR_CACHE = new Vector3f();

	private HubrisModelAnimation() {
	}

	public static void apply(
			float elapsedTicks,
			HubrisAnimationVariant variant,
			HumanoidArm mainArm,
			ModelPart head,
			ModelPart body,
			ModelPart rightArm,
			ModelPart leftArm,
			ModelPart rightLeg,
			ModelPart leftLeg
	) {
		AnimationDefinition animation = variant == HubrisAnimationVariant.HEAVY
				? mainArm == HumanoidArm.LEFT ? MIRRORED_HEAVY : HubrisActivation.HUBRIS_ACTIVATION_MACE
				: HubrisActivation.HUBRIS_ACTIVATION;
		float elapsedSeconds = Mth.clamp(elapsedTicks / 20.0F, 0.0F, animation.lengthInSeconds());

		for (Map.Entry<String, List<AnimationChannel>> entry : animation.boneAnimations().entrySet()) {
			ModelPart part = findPart(
					normalizeBoneName(entry.getKey()),
					head,
					body,
					rightArm,
					leftArm,
					rightLeg,
					leftLeg
			);
			if (part == null)
				continue;
			for (AnimationChannel channel : entry.getValue())
				channel.target().apply(part, sampleChannel(channel, elapsedSeconds));
		}
	}

	private static AnimationDefinition mirrorHeavyAnimation(AnimationDefinition source) {
		AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(source.lengthInSeconds());
		for (Map.Entry<String, List<AnimationChannel>> entry : source.boneAnimations().entrySet()) {
			String mirroredBone = mirroredBoneName(entry.getKey());
			for (AnimationChannel channel : entry.getValue()) {
				Keyframe[] sourceFrames = channel.keyframes();
				Keyframe[] mirroredFrames = new Keyframe[sourceFrames.length];
				for (int index = 0; index < sourceFrames.length; index++) {
					Keyframe frame = sourceFrames[index];
					Vector3f target = new Vector3f(frame.target());
					if (channel.target() == AnimationChannel.Targets.ROTATION) {
						target.set(target.x(), -target.y(), -target.z());
					} else if (channel.target() == AnimationChannel.Targets.POSITION) {
						target.set(-target.x(), target.y(), target.z());
					}
					mirroredFrames[index] = new Keyframe(frame.timestamp(), target, frame.interpolation());
				}
				builder.addAnimation(mirroredBone, new AnimationChannel(channel.target(), mirroredFrames));
			}
		}
		return builder.build();
	}

	private static Vector3f sampleChannel(AnimationChannel channel, float elapsedSeconds) {
		Keyframe[] keyframes = channel.keyframes();
		int currentIndex = Math.max(
				0,
				Mth.binarySearch(0, keyframes.length, index -> elapsedSeconds <= keyframes[index].timestamp()) - 1
		);
		int nextIndex = Math.min(keyframes.length - 1, currentIndex + 1);
		Keyframe current = keyframes[currentIndex];
		Keyframe next = keyframes[nextIndex];
		float progress = nextIndex == currentIndex
				? 0.0F
				: Mth.clamp(
						(elapsedSeconds - current.timestamp()) / (next.timestamp() - current.timestamp()),
						0.0F,
						1.0F
				);
		next.interpolation().apply(
				ANIMATION_VECTOR_CACHE,
				progress,
				keyframes,
				currentIndex,
				nextIndex,
				1.0F
		);
		return ANIMATION_VECTOR_CACHE;
	}

	private static ModelPart findPart(String name, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
		return switch (name) {
			case "head" -> head;
			case "body" -> body;
			case "rightarm" -> rightArm;
			case "leftarm" -> leftArm;
			case "rightleg" -> rightLeg;
			case "leftleg" -> leftLeg;
			default -> null;
		};
	}

	private static String mirroredBoneName(String name) {
		return switch (normalizeBoneName(name)) {
			case "leftarm" -> "right_arm";
			case "rightarm" -> "left_arm";
			case "leftleg" -> "right_leg";
			case "rightleg" -> "left_leg";
			default -> name;
		};
	}

	private static String normalizeBoneName(String name) {
		return name.replace(" ", "").replace("_", "").toLowerCase(Locale.ROOT);
	}
}
