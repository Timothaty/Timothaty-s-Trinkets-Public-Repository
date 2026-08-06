package net.timothaty.timothatystrinkets.client;

import net.timothaty.timothatystrinkets.client.model.animations.BlessingsAnimationAnimation;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;

import org.joml.Vector3f;

import java.util.List;
import java.util.Map.Entry;

public final class AnathemaVillagerBlessingModelAnimation {
	private static final Vector3f VECTOR_CACHE = new Vector3f();

	private AnathemaVillagerBlessingModelAnimation() {
	}

	public static void apply(AnimationState state, float ageInTicks, ModelPart root) {
		state.updateTime(ageInTicks, 1.0F);
		applyDefinition(BlessingsAnimationAnimation.BLESSINGS, state.getAccumulatedTime(), root);
	}

	private static void applyDefinition(AnimationDefinition animation, long elapsedMillis, ModelPart root) {
		float seconds = (float) elapsedMillis / 1000.0F;
		for (Entry<String, List<AnimationChannel>> entry : animation.boneAnimations().entrySet()) {
			ModelPart part = findPart(root, entry.getKey());
			if (part == null)
				continue;
			for (AnimationChannel channel : entry.getValue())
				applyChannel(part, channel, seconds);
		}
	}

	private static ModelPart findPart(ModelPart root, String name) {
		return switch (name) {
			case "head" -> root.getChild("head");
			case "nose" -> root.getChild("head").getChild("nose");
			case "body" -> root.getChild("body");
			case "arms" -> root.getChild("arms");
			default -> null;
		};
	}

	private static void applyChannel(ModelPart part, AnimationChannel channel, float elapsedSeconds) {
		Keyframe[] keyframes = channel.keyframes();
		int currentIndex = Math.max(0, Mth.binarySearch(0, keyframes.length, index -> elapsedSeconds <= keyframes[index].timestamp()) - 1);
		int nextIndex = Math.min(keyframes.length - 1, currentIndex + 1);
		Keyframe current = keyframes[currentIndex];
		Keyframe next = keyframes[nextIndex];
		float delta = elapsedSeconds - current.timestamp();
		float progress = nextIndex == currentIndex ? 0.0F : Mth.clamp(delta / (next.timestamp() - current.timestamp()), 0.0F, 1.0F);
		next.interpolation().apply(VECTOR_CACHE, progress, keyframes, currentIndex, nextIndex, 1.0F);
		channel.target().apply(part, VECTOR_CACHE);
	}
}
