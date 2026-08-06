package net.timothaty.timothatystrinkets.item;

import net.timothaty.timothatystrinkets.entity.SoulOrbEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class BottleOfSoulOrbItem extends Item {
	private static final String SOUL_VALUE_TAG = "SoulValue";

	public BottleOfSoulOrbItem() {
		super(new Item.Properties().stacksTo(16));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}

		Level level = context.getLevel();
		if (!level.isClientSide) {
			Direction face = context.getClickedFace();
			Vec3 outward = Vec3.atLowerCornerOf(face.getNormal()).scale(0.22D);
			Vec3 spawnPosition = context.getClickLocation().add(outward);
			SoulOrbEntity orb = new SoulOrbEntity(level, spawnPosition.x, spawnPosition.y, spawnPosition.z);
			orb.setSoulValue(getSoulValue(context.getItemInHand()));
			orb.setDeltaMovement(0.0D, 0.04D, 0.0D);
			level.addFreshEntity(orb);

			ItemStack replacement = ItemUtils.createFilledResult(
					context.getItemInHand(),
					player,
					new ItemStack(Items.GLASS_BOTTLE)
			);
			player.setItemInHand(context.getHand(), replacement);
		}

		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	public static ItemStack createFilledBottle(int soulValue) {
		ItemStack bottle = new ItemStack(TimothatysTrinketsModItems.BOTTLE_OF_SOUL_ORB.get());
		CompoundTag tag = bottle.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		tag.putInt(SOUL_VALUE_TAG, Math.max(1, soulValue));
		bottle.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return bottle;
	}

	public static int getSoulValue(ItemStack bottle) {
		CompoundTag tag = bottle.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
		return tag.contains(SOUL_VALUE_TAG) ? Math.max(1, tag.getInt(SOUL_VALUE_TAG)) : 1;
	}
}
