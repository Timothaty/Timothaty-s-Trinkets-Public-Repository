package net.timothaty.timothatystrinkets.mechanics.damnation_altar;

import net.timothaty.timothatystrinkets.block.entity.DamnationAltarBlockEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordSummonManager;
import net.timothaty.timothatystrinkets.util.DamnationAltarOfferDisplayHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class DamnationAltarInteractionHandler {
	public static final int MINIMUM_RELATION = 50;
	private static final int DAGGER_TRANSMUTATION_DAMAGE = 10;

	private DamnationAltarInteractionHandler() {
	}

	public static ItemInteractionResult handleItemInteraction(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof DamnationAltarBlockEntity altar && altar.hasExternalOffer()) {
			if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
				DamnationAltarOfferDisplayHandler.tryTakeOffer((ServerLevel) level, pos, serverPlayer);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		if (player.isShiftKeyDown() && isRitualDagger(held)) {
			if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
				DamnationAltarRelationHandler.displayRelation(serverPlayer);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		if (player.isShiftKeyDown()) {
			if (held.isEmpty()) tryBulkExtract();
			else tryBulkInsert();
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}

		DamnationAltarSlot slot = DamnationAltarSlotLayout.resolveSlot(state, hit);
		if (slot == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!(level.getBlockEntity(pos) instanceof DamnationAltarBlockEntity altar)) return ItemInteractionResult.FAIL;
		if (altar.isTransmuting()) return ItemInteractionResult.sidedSuccess(level.isClientSide);

		if (isRitualDagger(held)) {
			if (slot != DamnationAltarSlot.CENTER) return ItemInteractionResult.sidedSuccess(level.isClientSide);
			if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
				tryStartTransmutation((ServerLevel) level, serverPlayer, hand, altar);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}

		if (held.isEmpty()) {
			return hand == InteractionHand.MAIN_HAND && !player.getOffhandItem().isEmpty()
					? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
					: ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (level.isClientSide) return ItemInteractionResult.SUCCESS;
		ServerLevel serverLevel = (ServerLevel) level;
		ServerPlayer serverPlayer = (ServerPlayer) player;
		if (!canInsertOrStart(serverLevel, pos, altar)) return ItemInteractionResult.SUCCESS;
		if (DamnationAltarRelationHandler.getOrInitRelation(serverPlayer) < MINIMUM_RELATION) {
			DamnationAltarPunishmentService.punishLowRelation(serverLevel, serverPlayer, pos);
			return ItemInteractionResult.SUCCESS;
		}
		if (altar.insertOne(slot, held, serverPlayer.getAbilities().instabuild)) {
			playItemInteractionSound(serverLevel, pos);
		}
		return ItemInteractionResult.SUCCESS;
	}

	public static InteractionResult handleEmptyHandInteraction(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof DamnationAltarBlockEntity altar && altar.hasExternalOffer()) {
			if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
				DamnationAltarOfferDisplayHandler.tryTakeOffer((ServerLevel) level, pos, serverPlayer);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		DamnationAltarSlot slot = DamnationAltarSlotLayout.resolveSlot(state, hit);
		if (slot == null) return InteractionResult.PASS;
		if (!(level.getBlockEntity(pos) instanceof DamnationAltarBlockEntity altar)) return InteractionResult.FAIL;
		if (altar.isTransmuting()) return InteractionResult.sidedSuccess(level.isClientSide);
		if (level.isClientSide) return InteractionResult.SUCCESS;

		ItemStack extracted = altar.extractOne(slot);
		if (!extracted.isEmpty()) {
			playItemInteractionSound((ServerLevel) level, pos);
		}
		if (!extracted.isEmpty() && !player.getInventory().add(extracted)) {
			player.drop(extracted, false);
		}
		return InteractionResult.SUCCESS;
	}

	private static void tryStartTransmutation(ServerLevel level, ServerPlayer player, InteractionHand hand, DamnationAltarBlockEntity altar) {
		BlockPos pos = altar.getBlockPos();
		if (!altar.canAcceptTransmutationInteraction()) return;
		if (!canInsertOrStart(level, pos, altar)) return;
		if (DamnationAltarRelationHandler.getOrInitRelation(player) < MINIMUM_RELATION) {
			DamnationAltarPunishmentService.punishLowRelation(level, player, pos);
			return;
		}

		altar.getMatchingRecipe(level).ifPresent(recipe -> {
			if (!altar.startTransmutation(level, recipe)) return;
			EquipmentSlot equipmentSlot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
			player.getItemInHand(hand).hurtAndBreak(DAGGER_TRANSMUTATION_DAMAGE, player, equipmentSlot);
		});
	}

	private static boolean canInsertOrStart(ServerLevel level, BlockPos pos, DamnationAltarBlockEntity altar) {
		return !altar.isTransmuting()
				&& !altar.hasExternalOffer()
				&& !DebtlordSummonManager.isAltarActive(level, pos)
				&& !DamnationAltarSacrificeRouter.isBloodRitualActive(level, pos);
	}

	private static boolean isRitualDagger(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() == TimothatysTrinketsModItems.RITUAL_DAGGER.get();
	}

	private static void playItemInteractionSound(ServerLevel level, BlockPos pos) {
		level.playSound(
				null,
				pos.getX() + 0.5D,
				pos.getY() + 0.5D,
				pos.getZ() + 0.5D,
				SoundEvents.ARMOR_EQUIP_LEATHER,
				SoundSource.BLOCKS,
				1.0F,
				1.0F
		);
	}

	private static void tryBulkInsert() {
	}

	private static void tryBulkExtract() {
	}

}
