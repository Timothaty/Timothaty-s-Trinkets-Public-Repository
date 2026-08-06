package net.timothaty.timothatystrinkets.client.morgenshtern;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import net.minecraft.client.model.BreezeModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(
		modid = TimothatysTrinketsMod.MODID,
		value = Dist.CLIENT
)
public final class MorgenshternDecapitationRenderer {
	private static final Map<LivingEntity, SavedVisibility> SAVED_VISIBILITY =
			Collections.synchronizedMap(new IdentityHashMap<>());

	private MorgenshternDecapitationRenderer() {
	}

	@SubscribeEvent(
			priority = EventPriority.LOWEST,
			receiveCanceled = true
	)
	public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
		LivingEntity entity = event.getEntity();
		EntityModel<?> model = event.getRenderer().getModel();
		ModelPart head = findHead(model);
		restoreStaleVisibility(entity, head);
		if (event.isCanceled()
				|| !MorgenshternDecapitationClientState.isDecapitated(entity))
			return;

		if (head == null)
			return;

		ModelPart hat = model instanceof HumanoidModel<?> humanoidModel
				? humanoidModel.hat
				: null;
		SAVED_VISIBILITY.put(
				entity,
				new SavedVisibility(
						head,
						head.visible,
						hat,
						hat != null && hat.visible
				)
		);
		head.visible = false;
		if (hat != null) {
			hat.visible = false;
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
		restoreSavedVisibility(SAVED_VISIBILITY.remove(event.getEntity()));
	}

	public static void clearAndRestore() {
		synchronized (SAVED_VISIBILITY) {
			for (SavedVisibility saved : SAVED_VISIBILITY.values()) {
				restoreSavedVisibility(saved);
			}
			SAVED_VISIBILITY.clear();
		}
	}

	private static void restoreStaleVisibility(LivingEntity entity, ModelPart currentHead) {
		synchronized (SAVED_VISIBILITY) {
			Iterator<Map.Entry<LivingEntity, SavedVisibility>> iterator = SAVED_VISIBILITY.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<LivingEntity, SavedVisibility> entry = iterator.next();
				SavedVisibility saved = entry.getValue();
				if (entry.getKey() != entity && (currentHead == null || saved.head() != currentHead)) {
					continue;
				}
				restoreSavedVisibility(saved);
				iterator.remove();
			}
		}
	}

	private static void restoreSavedVisibility(SavedVisibility saved) {
		if (saved == null)
			return;

		saved.head().visible = saved.headVisible();
		if (saved.hat() != null) {
			saved.hat().visible = saved.hatVisible();
		}
	}

	private static ModelPart findHead(EntityModel<?> model) {
		if (model instanceof HeadedModel headedModel) {
			return headedModel.getHead();
		}
		if (model instanceof BreezeModel<?> breezeModel) {
			return breezeModel.head();
		}
		if (model instanceof HierarchicalModel<?> hierarchicalModel) {
			ModelPart root = hierarchicalModel.root();
			if (root.hasChild("head")) {
				return root.getChild("head");
			}
		}
		return null;
	}

	private record SavedVisibility(
			ModelPart head,
			boolean headVisible,
			ModelPart hat,
			boolean hatVisible
	) {
	}
}
