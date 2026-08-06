package net.timothaty.timothatystrinkets.client.stunned;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public final class StunnedHumanoidHeadAnimationClient {
	private static final float HEAD_DOWN_RADIANS = 42.0F * Mth.DEG_TO_RAD;
	private static final float HEAD_SWAY_AMPLITUDE_RADIANS = 14.0F * Mth.DEG_TO_RAD;
	private static final float HEAD_SWAY_SPEED = 0.34F;

	private static final Map<Integer, SavedHeadRotations> SAVED_ROTATIONS = new HashMap<>();

	private StunnedHumanoidHeadAnimationClient() {
	}

	@SubscribeEvent
	public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
		LivingEntity entity = event.getEntity();
		if (entity == null || !entity.hasEffect(TimothatysTrinketsModMobEffects.STUNNED))
			return;
		if (!(event.getRenderer().getModel() instanceof HumanoidModel<?> model))
			return;

		ModelPart head = model.head;
		float partialTick = event.getPartialTick();
		float time = entity.tickCount + partialTick;
		float sway = Mth.sin(time * HEAD_SWAY_SPEED) * HEAD_SWAY_AMPLITUDE_RADIANS;

		SAVED_ROTATIONS.put(entity.getId(), new SavedHeadRotations(head.xRot, head.yRot, head.zRot));

		head.xRot = HEAD_DOWN_RADIANS;
		head.yRot = sway;
		head.zRot = sway * 0.35F;
	}

	@SubscribeEvent
	public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
		LivingEntity entity = event.getEntity();
		if (entity == null || !(event.getRenderer().getModel() instanceof HumanoidModel<?> model))
			return;

		SavedHeadRotations saved = SAVED_ROTATIONS.remove(entity.getId());
		if (saved == null)
			return;

		ModelPart head = model.head;
		head.xRot = saved.xRot();
		head.yRot = saved.yRot();
		head.zRot = saved.zRot();
	}

	private record SavedHeadRotations(float xRot, float yRot, float zRot) {
	}
}
