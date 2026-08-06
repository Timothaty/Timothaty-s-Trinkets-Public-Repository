package net.timothaty.timothatystrinkets.mechanics.necromancer;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.blight.BlightSpreadHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class UnholyAuraEvents {
	private static final ResourceLocation UNDEAD_SPEED_ID =
		ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "unholy_aura_undead_speed");
	private static final ResourceLocation LIVING_ARMOR_ID =
		ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "unholy_aura_living_armor");

	private static final double UNDEAD_SPEED_BONUS = 0.15D;
	private static final double LIVING_ARMOR_REDUCTION = -2.0D;
	private static final String NBT_AURA_STATE = "tt_unholy_aura_state";
	private static final int AURA_STATE_NONE = 0;
	private static final int AURA_STATE_UNDEAD = 1;
	private static final int AURA_STATE_LIVING = 2;

	private UnholyAuraEvents() {
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (!event.getLevel().isClientSide()
				&& event.getEntity() instanceof LivingEntity entity
				&& !entity.hasEffect(TimothatysTrinketsModMobEffects.UNHOLY_AURA)
				&& getStoredAuraState(entity) != AURA_STATE_NONE) {
			cleanupUnholyAura(entity);
		}
	}

	@SubscribeEvent
	public static void onMobEffectAdded(MobEffectEvent.Added event) {
		if (!isUnholyAura(event.getEffectInstance())) {
			return;
		}

		refreshUnholyAuraModifiers(event.getEntity());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onMobEffectRemove(MobEffectEvent.Remove event) {
		if (event.isCanceled() || event.getEffect().value() != TimothatysTrinketsModMobEffects.UNHOLY_AURA.get()) {
			return;
		}

		cleanupUnholyAura(event.getEntity());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onMobEffectExpired(MobEffectEvent.Expired event) {
		if (event.isCanceled() || !isUnholyAura(event.getEffectInstance())) {
			return;
		}

		cleanupUnholyAura(event.getEntity());
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity == null) {
			return;
		}

		if (entity.hasEffect(TimothatysTrinketsModMobEffects.UNHOLY_AURA) && !entity.getType().is(EntityTypeTags.UNDEAD)) {
			blightGroundUnder(entity);
		}

		cleanupUnholyAura(entity);
	}

	public static void refreshUnholyAuraModifiers(LivingEntity entity) {
		if (entity.level().isClientSide() || isSpectator(entity)) {
			return;
		}

		int desiredState = getDesiredAuraState(entity);
		int storedState = getStoredAuraState(entity);

		if (desiredState == AURA_STATE_NONE) {
			if (storedState != AURA_STATE_NONE) {
				cleanupUnholyAura(entity);
			}
			return;
		}

		if (storedState != desiredState) {
			removeAuraModifiers(entity);
			entity.getPersistentData().putInt(NBT_AURA_STATE, desiredState);
		}

		ensureAuraStateModifiers(entity, desiredState);
	}

	private static void ensureAuraStateModifiers(LivingEntity entity, int auraState) {
		boolean isUndead = auraState == AURA_STATE_UNDEAD;

		updateAttributeModifier(
			entity,
			Attributes.MOVEMENT_SPEED,
			UNDEAD_SPEED_ID,
			UNDEAD_SPEED_BONUS,
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
			isUndead
		);
		updateAttributeModifier(
			entity,
			Attributes.ARMOR,
			LIVING_ARMOR_ID,
			LIVING_ARMOR_REDUCTION,
			AttributeModifier.Operation.ADD_VALUE,
			!isUndead
		);
	}

	private static void cleanupUnholyAura(LivingEntity entity) {
		removeAuraModifiers(entity);
		entity.getPersistentData().remove(NBT_AURA_STATE);
	}

	private static void removeAuraModifiers(LivingEntity entity) {
		removeAttributeModifier(entity, Attributes.MOVEMENT_SPEED, UNDEAD_SPEED_ID);
		removeAttributeModifier(entity, Attributes.ARMOR, LIVING_ARMOR_ID);
	}

	private static int getDesiredAuraState(LivingEntity entity) {
		if (!entity.hasEffect(TimothatysTrinketsModMobEffects.UNHOLY_AURA)) {
			return AURA_STATE_NONE;
		}

		return entity.getType().is(EntityTypeTags.UNDEAD) ? AURA_STATE_UNDEAD : AURA_STATE_LIVING;
	}

	private static int getStoredAuraState(LivingEntity entity) {
		CompoundTag data = entity.getPersistentData();
		return data.contains(NBT_AURA_STATE) ? data.getInt(NBT_AURA_STATE) : AURA_STATE_NONE;
	}

	private static void updateAttributeModifier(
		LivingEntity entity,
		Holder<Attribute> attribute,
		ResourceLocation modifierId,
		double amount,
		AttributeModifier.Operation operation,
		boolean shouldHaveModifier
	) {
		AttributeInstance attributeInstance = entity.getAttribute(attribute);
		if (attributeInstance == null) {
			return;
		}

		boolean hasModifier = attributeInstance.getModifier(modifierId) != null;
		if (shouldHaveModifier && !hasModifier) {
			attributeInstance.addTransientModifier(new AttributeModifier(modifierId, amount, operation));
		} else if (!shouldHaveModifier && hasModifier) {
			attributeInstance.removeModifier(modifierId);
		}
	}

	private static void removeAttributeModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation modifierId) {
		AttributeInstance attributeInstance = entity.getAttribute(attribute);
		if (attributeInstance != null && attributeInstance.getModifier(modifierId) != null) {
			attributeInstance.removeModifier(modifierId);
		}
	}

	private static void blightGroundUnder(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		BlockPos groundPos = findBlightableGroundPos(serverLevel, entity);
		if (groundPos == null) {
			return;
		}

		BlightSpreadHelper.infectBlock(serverLevel, groundPos, groundPos.getY());
	}

	private static BlockPos findBlightableGroundPos(ServerLevel serverLevel, LivingEntity entity) {
		BlockPos groundPos = BlockPos.containing(entity.getX(), entity.getBoundingBox().minY - 0.1D, entity.getZ());
		BlockState groundState = serverLevel.getBlockState(groundPos);
		if (BlightSpreadHelper.canBeBlighted(groundState)) {
			return groundPos;
		}

		BlockPos belowGroundPos = groundPos.below();
		BlockState belowGroundState = serverLevel.getBlockState(belowGroundPos);
		return BlightSpreadHelper.canBeBlighted(belowGroundState) ? belowGroundPos : null;
	}

	private static boolean isUnholyAura(MobEffectInstance instance) {
		return instance != null && instance.getEffect().value() == TimothatysTrinketsModMobEffects.UNHOLY_AURA.get();
	}

	private static boolean isSpectator(LivingEntity entity) {
		return entity instanceof Player player && player.isSpectator();
	}
}
