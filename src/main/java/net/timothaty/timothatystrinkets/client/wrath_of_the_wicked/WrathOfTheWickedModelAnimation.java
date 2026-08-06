package net.timothaty.timothatystrinkets.client.wrath_of_the_wicked;

import net.timothaty.timothatystrinkets.client.model.animations.WrathOfTheWickedAnimation;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class WrathOfTheWickedModelAnimation {
	private static final Vector3f ANIMATION_VECTOR_CACHE = new Vector3f();

	private WrathOfTheWickedModelAnimation() {
	}

	public static void apply(
			float elapsedTicks,
			ModelPart head,
			ModelPart body,
			ModelPart rightArm,
			ModelPart leftArm,
			ModelPart rightLeg,
			ModelPart leftLeg
	) {
		AnimationDefinition animation = WrathOfTheWickedAnimation.WRATH_OF_THE_WICKED;
		float elapsedSeconds = Mth.clamp(elapsedTicks / 20.0F, 0.0F, animation.lengthInSeconds());

		for (Map.Entry<String, List<AnimationChannel>> entry : animation.boneAnimations().entrySet()) {
			String boneName = normalizeBoneName(entry.getKey());
			for (AnimationChannel channel : entry.getValue()) {
				if ("waist".equals(boneName)) {
					applyChannel(body, channel, elapsedSeconds);
					applyChannel(head, channel, elapsedSeconds);
					applyChannel(rightArm, channel, elapsedSeconds);
					applyChannel(leftArm, channel, elapsedSeconds);
					continue;
				}

				ModelPart part = findHumanoidPart(
						boneName, head, body, rightArm, leftArm, rightLeg, leftLeg
				);
				if (part != null) {
					applyChannel(part, channel, elapsedSeconds);
				}
			}
		}
	}

	private static void applyChannel(ModelPart part, AnimationChannel channel, float elapsedSeconds) {
		Keyframe[] keyframes = channel.keyframes();
		int currentIndex = Math.max(
				0,
				Mth.binarySearch(0, keyframes.length, index -> elapsedSeconds <= keyframes[index].timestamp()) - 1
		);
		int nextIndex = Math.min(keyframes.length - 1, currentIndex + 1);
		Keyframe current = keyframes[currentIndex];
		Keyframe next = keyframes[nextIndex];
		float keyframeProgress = nextIndex == currentIndex
				? 0.0F
				: Mth.clamp(
						(elapsedSeconds - current.timestamp()) / (next.timestamp() - current.timestamp()),
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
		channel.target().apply(part, ANIMATION_VECTOR_CACHE);
	}

	private static ModelPart findHumanoidPart(
			String boneName,
			ModelPart head,
			ModelPart body,
			ModelPart rightArm,
			ModelPart leftArm,
			ModelPart rightLeg,
			ModelPart leftLeg
	) {
		return switch (boneName) {
			case "head" -> head;
			case "body" -> body;
			case "rightarm" -> rightArm;
			case "leftarm" -> leftArm;
			case "rightleg" -> rightLeg;
			case "leftleg" -> leftLeg;
			default -> null;
		};
	}

	private static String normalizeBoneName(String boneName) {
		return boneName.replace(" ", "").replace("_", "").toLowerCase(Locale.ROOT);
	}
}
