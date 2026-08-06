package net.timothaty.timothatystrinkets.mechanics.striker_acquisition;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class StrikerForgingAttentionEvents {
	private static final Map<Villager, Vec3> ACTIVE_TARGETS = new WeakHashMap<>();

	private StrikerForgingAttentionEvents() {
	}

	static void track(Villager villager, Vec3 target) {
		if (villager != null && target != null)
			ACTIVE_TARGETS.put(villager, target);
	}

	static void clear(Villager villager) {
		if (villager != null)
			ACTIVE_TARGETS.remove(villager);
	}

	@SubscribeEvent
	public static void onEntityTickPost(EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof Villager villager) || villager.level().isClientSide())
			return;

		Vec3 target = ACTIVE_TARGETS.get(villager);
		if (target == null)
			return;
		if (StrikerCommissionData.getStage(villager) != StrikerCommissionStage.FORGING) {
			ACTIVE_TARGETS.remove(villager);
			return;
		}

		forceLookAt(villager, target);
	}

	private static void forceLookAt(Villager villager, Vec3 target) {
		double deltaX = target.x - villager.getX();
		double deltaY = target.y - villager.getEyeY();
		double deltaZ = target.z - villager.getZ();
		double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
		float yaw = (float) (Mth.atan2(deltaZ, deltaX) * Mth.RAD_TO_DEG) - 90.0F;
		float pitch = Mth.clamp((float) (-(Mth.atan2(deltaY, horizontalDistance) * Mth.RAD_TO_DEG)), -60.0F, 60.0F);

		villager.getLookControl().setLookAt(target.x, target.y, target.z, 90.0F, 90.0F);
		villager.setYRot(yaw);
		villager.yRotO = yaw;
		villager.yBodyRot = yaw;
		villager.yBodyRotO = yaw;
		villager.setYHeadRot(yaw);
		villager.yHeadRotO = yaw;
		villager.setXRot(pitch);
		villager.xRotO = pitch;
	}
}
