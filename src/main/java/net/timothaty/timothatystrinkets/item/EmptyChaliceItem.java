package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EmptyChaliceItem extends Item {
	private static final float FILL_CHANCE = 0.75F;
	private static final float DYING_HEALTH_RATIO = 0.25F;
	private static final TagKey<EntityType<?>> CAN_COLLECT_REF_WINE = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "can_collect_ref_wine")
	);

	public EmptyChaliceItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (stack.isEmpty() || !target.getType().is(CAN_COLLECT_REF_WINE) || !isDying(target)) {
			return InteractionResult.PASS;
		}

		Level level = player.level();
		if (!level.isClientSide() && level.random.nextFloat() < FILL_CHANCE) {
			player.setItemInHand(hand, new ItemStack(TimothatysTrinketsModItems.REFRESHING_CHALICE.get()));
			level.playSound(null, target.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.9F, 1.1F);
		}

		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	private static boolean isDying(LivingEntity target) {
		if (target.isDeadOrDying()) {
			return true;
		}

		float maxHealth = target.getMaxHealth();
		return maxHealth > 0.0F && target.getHealth() <= maxHealth * DYING_HEALTH_RATIO;
	}
}
