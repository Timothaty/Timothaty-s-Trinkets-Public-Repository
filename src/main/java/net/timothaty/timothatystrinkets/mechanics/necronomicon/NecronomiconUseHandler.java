package net.timothaty.timothatystrinkets.mechanics.necronomicon;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;
import net.timothaty.timothatystrinkets.mechanics.holy_rosarium.HolyRosariumHelper;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class NecronomiconUseHandler {
	private static final int BASE_COOLDOWN_SECONDS = 15;
	private static final double COOLDOWN_HEALTH_RATIO = 0.05D;
	private static final int BASE_DURABILITY_COST = 5;
	private static final double DURABILITY_HEALTH_RATIO = 0.03D;
	private static final int BASE_EFFECT_SECONDS = 25;
	private static final double EFFECT_HEALTH_RATIO = 0.25D;
	private static final TagKey<EntityType<?>> UNDEADIFY_RESTRICTED = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "undeadify_restricted")
	);

	private NecronomiconUseHandler() {}

	public static InteractionResult useOnEntity(Level level, Player player, LivingEntity target, ItemStack stack) {
		if (player == null || target == null || stack == null || stack.isEmpty()) return InteractionResult.PASS;
		if (HolyRosariumHelper.isUnholyRelicSuppressed(player, stack)) return InteractionResult.FAIL;
		if (!player.isShiftKeyDown()) return InteractionResult.PASS;
		if (target.hasEffect(TimothatysTrinketsModMobEffects.UNDEADIFICATION)) return InteractionResult.PASS;
		if (target.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE) || target.getType().is(UNDEADIFY_RESTRICTED)) return InteractionResult.PASS;

		Item usedItem = stack.getItem();
		if (player.getCooldowns().isOnCooldown(usedItem)) return InteractionResult.FAIL;

		double targetHealth = Math.max(0.0D, target.getMaxHealth());
		int cooldownSeconds = BASE_COOLDOWN_SECONDS + Mth.floor(targetHealth * COOLDOWN_HEALTH_RATIO + 0.5D);
		int cooldownTicks = Math.max(1, cooldownSeconds * 20);

		int durabilityLoss = BASE_DURABILITY_COST + Mth.floor(targetHealth * DURABILITY_HEALTH_RATIO + 0.5D);
		int effectSeconds = BASE_EFFECT_SECONDS + Mth.floor(targetHealth * EFFECT_HEALTH_RATIO + 0.5D);
		int effectTicks = Math.max(1, effectSeconds * 20);

		if (!level.isClientSide()) {
			target.addEffect(new MobEffectInstance(TimothatysTrinketsModMobEffects.UNDEADIFICATION, effectTicks, 0, false, true, true));
			player.getCooldowns().addCooldown(usedItem, cooldownTicks);
			damageItem(stack, player, durabilityLoss);
		}

		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	private static void damageItem(ItemStack stack, Player player, int amount) {
		if (amount <= 0 || stack.isEmpty() || player.getAbilities().instabuild) return;

		int newDamage = stack.getDamageValue() + amount;
		if (newDamage >= stack.getMaxDamage()) {
			stack.shrink(1);
		} else {
			stack.setDamageValue(newDamage);
		}
	}

}
