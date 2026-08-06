package net.timothaty.timothatystrinkets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID, value = Dist.CLIENT)
public class UndeadificationRenderShakeClient {
	private static final float BASE_FREQ = 3.6f;
	private static final float JITTER_FREQ = 9.5f;

	private static final float SIDE_TRANSLATE = 0.014f;
	private static final float UP_TRANSLATE = 0.0045f;

	private static final float YAW_DEGREES = 1.2f;
	private static final float ROLL_DEGREES = 0.55f;
	private static final float PITCH_DEGREES = 0.38f;

	private static final Set<LivingEntity> PUSHED_ENTITIES = Collections.newSetFromMap(new IdentityHashMap<>());

	@SubscribeEvent
	public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
		applyShake(event.getEntity(), event.getPartialTick(), event.getPoseStack());
	}

	@SubscribeEvent
	public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
		popShake(event.getEntity(), event.getPoseStack());
	}

	@SubscribeEvent
	public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
		LivingEntity entity = event.getEntity();

		if (entity instanceof Player) {
			return;
		}

		applyShake(entity, event.getPartialTick(), event.getPoseStack());
	}

	@SubscribeEvent
	public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
		LivingEntity entity = event.getEntity();

		if (entity instanceof Player) {
			return;
		}

		popShake(entity, event.getPoseStack());
	}

	private static void applyShake(LivingEntity entity, float partialTick, PoseStack poseStack) {
		float strength = UndeadificationShakeHelper.getShakeStrength(entity);
		if (strength <= 0.0F) {
			return;
		}

		poseStack.pushPose();
		PUSHED_ENTITIES.add(entity);

		float time = entity.tickCount + partialTick;
		float baseWave = Mth.cos(time * BASE_FREQ);
		float jitterWave = Mth.sin(time * JITTER_FREQ) * 0.35f;
		float combined = baseWave + jitterWave;

		poseStack.translate(combined * SIDE_TRANSLATE * strength, Math.abs(jitterWave) * UP_TRANSLATE * strength, 0.0D);
		poseStack.mulPose(Axis.YP.rotationDegrees(combined * YAW_DEGREES * strength));
		poseStack.mulPose(Axis.ZP.rotationDegrees(combined * ROLL_DEGREES * strength));
		poseStack.mulPose(Axis.XP.rotationDegrees(jitterWave * PITCH_DEGREES * strength));
	}

	private static void popShake(LivingEntity entity, PoseStack poseStack) {
		if (PUSHED_ENTITIES.remove(entity)) {
			poseStack.popPose();
		}
	}
}
