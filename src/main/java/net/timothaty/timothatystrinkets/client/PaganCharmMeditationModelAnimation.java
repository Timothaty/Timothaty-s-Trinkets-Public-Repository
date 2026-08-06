package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.client.model.animations.MeditationAnimationAnimation;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

@OnlyIn(Dist.CLIENT)
public final class PaganCharmMeditationModelAnimation {
	private static final Vector3f ANIMATION_VECTOR_CACHE = new Vector3f();

	private PaganCharmMeditationModelAnimation() {
	}

	public static void applyMeditate(AnimationState animationState, float ageInTicks, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
		animationState.updateTime(ageInTicks, 1.0F);
		apply(MeditationAnimationAnimation.MEDITATE, animationState.getAccumulatedTime(), head, body, rightArm, leftArm, rightLeg, leftLeg);
	}

	public static void applyLoop(AnimationState animationState, float ageInTicks, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
		animationState.updateTime(ageInTicks, 1.0F);
		apply(MeditationAnimationAnimation.MEDITATE_IDLE, animationState.getAccumulatedTime(), head, body, rightArm, leftArm, rightLeg, leftLeg);
	}

	private static void apply(AnimationDefinition animation, long elapsedMillis, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
		float elapsedSeconds = getElapsedSeconds(animation, elapsedMillis);

		for (Entry<String, List<AnimationChannel>> entry : animation.boneAnimations().entrySet()) {
			ModelPart part = findHumanoidPart(entry.getKey(), head, body, rightArm, leftArm, rightLeg, leftLeg);
			if (part == null)
				continue;

			for (AnimationChannel channel : entry.getValue()) {
				applyChannel(part, channel, elapsedSeconds);
			}
		}
	}

	private static void applyChannel(ModelPart part, AnimationChannel channel, float elapsedSeconds) {
		Keyframe[] keyframes = channel.keyframes();
		int currentIndex = Math.max(0, Mth.binarySearch(0, keyframes.length, index -> elapsedSeconds <= keyframes[index].timestamp()) - 1);
		int nextIndex = Math.min(keyframes.length - 1, currentIndex + 1);
		Keyframe current = keyframes[currentIndex];
		Keyframe next = keyframes[nextIndex];
		float delta = elapsedSeconds - current.timestamp();
		float keyframeDelta = nextIndex != currentIndex ? Mth.clamp(delta / (next.timestamp() - current.timestamp()), 0.0F, 1.0F) : 0.0F;

		next.interpolation().apply(ANIMATION_VECTOR_CACHE, keyframeDelta, keyframes, currentIndex, nextIndex, 1.0F);
		channel.target().apply(part, ANIMATION_VECTOR_CACHE);
	}

	private static ModelPart findHumanoidPart(String boneName, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
		String key = boneName.replace(" ", "").replace("_", "").toLowerCase(Locale.ROOT);
		return switch (key) {
			case "head" -> head;
			case "body" -> body;
			case "rightarm" -> rightArm;
			case "leftarm" -> leftArm;
			case "rightleg" -> rightLeg;
			case "leftleg" -> leftLeg;
			default -> null;
		};
	}

	private static float getElapsedSeconds(AnimationDefinition animation, long elapsedMillis) {
		float elapsedSeconds = (float) elapsedMillis / 1000.0F;
		return animation.looping() ? elapsedSeconds % animation.lengthInSeconds() : elapsedSeconds;
	}
}
