package net.timothaty.timothatystrinkets.mechanics.effects;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class DesolatedEffectEvents {
	private DesolatedEffectEvents() {
	}

	private static final ResourceLocation DESOLATION_PARTICLE_ID =
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "desolation");

	private static final ResourceLocation ARMOR_REDUCTION_ID =
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "desolated_armor_reduction");

	private static final ResourceLocation MOVEMENT_SPEED_REDUCTION_ID =
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "desolated_movement_speed_reduction");

	private static final double ARMOR_REDUCTION_MULTIPLIER = -0.20D;
	private static final double MOVEMENT_SPEED_REDUCTION_MULTIPLIER = -0.20D;
	private static final String NBT_NEXT_SOUL_DAMAGE_TICK = "ttr_desolated_next_soul_damage_tick";

	private static final int PARTICLE_KEEPALIVE_INTERVAL_TICKS = 8;

	public static void tickEffect(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel server))
			return;

		updateMechanicalDebuffs(entity, true);

		if (isSpectator(entity))
			return;

		if ((entity.tickCount % PARTICLE_KEEPALIVE_INTERVAL_TICKS) != 0)
			return;

		spawnDesolationParticleKeepalive(server, entity);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingHeal(LivingHealEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity == null || isMechanicallyImmune(entity))
			return;

		if (hasDesolated(entity)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onMobEffectRemove(MobEffectEvent.Remove event) {
		if (event.getEffect().value() != TimothatysTrinketsModMobEffects.DESOLATED.get())
			return;

		if (event.getCure() != null) {
			event.setCanceled(true);
			return;
		}

	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onMobEffectRemoveCleanup(MobEffectEvent.Remove event) {
		if (event.isCanceled() || event.getEffect().value() != TimothatysTrinketsModMobEffects.DESOLATED.get())
			return;

		cleanupMechanicalDebuffs(event.getEntity());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onMobEffectExpired(MobEffectEvent.Expired event) {
		if (event.isCanceled() || !isDesolated(event.getEffectInstance()))
			return;

		cleanupMechanicalDebuffs(event.getEntity());
	}

	private static void updateMechanicalDebuffs(LivingEntity entity, boolean hasDesolated) {
		boolean shouldApply = hasDesolated && !isMechanicallyImmune(entity);

		updateAttributeModifier(
				entity,
				Attributes.ARMOR,
				ARMOR_REDUCTION_ID,
				ARMOR_REDUCTION_MULTIPLIER,
				shouldApply
		);

		updateAttributeModifier(
				entity,
				Attributes.MOVEMENT_SPEED,
				MOVEMENT_SPEED_REDUCTION_ID,
				MOVEMENT_SPEED_REDUCTION_MULTIPLIER,
				shouldApply
		);
	}

	private static void updateAttributeModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation modifierId, double amount, boolean shouldHaveModifier) {
		AttributeInstance attributeInstance = entity.getAttribute(attribute);
		if (attributeInstance == null)
			return;

		boolean hasModifier = attributeInstance.getModifier(modifierId) != null;

		if (shouldHaveModifier && !hasModifier) {
			attributeInstance.addTransientModifier(new AttributeModifier(
					modifierId,
					amount,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
			));
		} else if (!shouldHaveModifier && hasModifier) {
			attributeInstance.removeModifier(modifierId);
		}
	}

	private static void cleanupMechanicalDebuffs(LivingEntity entity) {
		if (entity == null)
			return;

		removeAttributeModifier(entity, Attributes.ARMOR, ARMOR_REDUCTION_ID);
		removeAttributeModifier(entity, Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_REDUCTION_ID);
		entity.getPersistentData().remove(NBT_NEXT_SOUL_DAMAGE_TICK);
	}

	private static void removeAttributeModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation modifierId) {
		AttributeInstance attributeInstance = entity.getAttribute(attribute);
		if (attributeInstance != null && attributeInstance.getModifier(modifierId) != null) {
			attributeInstance.removeModifier(modifierId);
		}
	}

	private static void spawnDesolationParticleKeepalive(ServerLevel server, LivingEntity entity) {
		var particleType = BuiltInRegistries.PARTICLE_TYPE.get(DESOLATION_PARTICLE_ID);
		if (!(particleType instanceof SimpleParticleType simple))
			return;

		server.sendParticles(
				simple,
				entity.getX(), entity.getY(), entity.getZ(),
				0,
				entity.getId(), 0.0D, 0.0D,
				1.0D
		);
	}

	private static boolean hasDesolated(LivingEntity entity) {
		return entity.hasEffect(TimothatysTrinketsModMobEffects.DESOLATED);
	}

	private static boolean isDesolated(MobEffectInstance instance) {
		return instance != null && instance.getEffect().value() == TimothatysTrinketsModMobEffects.DESOLATED.get();
	}

	private static boolean isMechanicallyImmune(LivingEntity entity) {
		return entity instanceof Player player && (player.isCreative() || player.isSpectator());
	}

	private static boolean isSpectator(LivingEntity entity) {
		return entity instanceof Player player && player.isSpectator();
	}
}
