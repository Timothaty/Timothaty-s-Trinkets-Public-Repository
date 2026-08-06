package net.timothaty.timothatystrinkets.mixin;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.item.GoldenHoneyCombItem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeehiveBlock.class)
public abstract class GoldenHoneyCombBeehiveMixin {

	private static final float BASE_GOLDEN_HONEY_COMB_DROP_CHANCE = 0.02F;
	private static final float HIVES_BOUNTY_GOLDEN_HONEY_COMB_DROP_BONUS = 0.01F;

	@Inject(
			method = "useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/ItemInteractionResult;",
			at = @At("HEAD"),
			require = 0
	)
	private void timothatys_trinkets$handleGoldenHoneyCombHiveHarvestBonuses(
			ItemStack stack,
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			InteractionHand hand,
			BlockHitResult hitResult,
			CallbackInfoReturnable<ItemInteractionResult> cir
	) {
		if (level.isClientSide()) {
			return;
		}

		if (state.getValue(BeehiveBlock.HONEY_LEVEL) < 5) {
			return;
		}

		if (stack.is(Items.SHEARS)) {
			tryDropNewGoldenHoneyComb(level, pos, player);
			tryDropExtraHoneycombsFromHivesBounty(level, pos, player);
			return;
		}

		if (stack.is(Items.GLASS_BOTTLE)) {
			tryRefundBottleFromHivesBounty(level, pos, player);
		}
	}

	private static void tryDropNewGoldenHoneyComb(Level level, BlockPos pos, Player player) {
		float dropChance = BASE_GOLDEN_HONEY_COMB_DROP_CHANCE;
		if (getBestHivesBountyLevel(player) >= 3) {
			dropChance += HIVES_BOUNTY_GOLDEN_HONEY_COMB_DROP_BONUS;
		}

		if (level.random.nextFloat() >= dropChance) {
			return;
		}

		ItemStack goldenComb = new ItemStack(TimothatysTrinketsModItems.GOLDEN_HONEY_COMB.get());
		GoldenHoneyCombItem.setCharge(goldenComb, 1);
		Block.popResource(level, pos, goldenComb);
	}

	private static void tryDropExtraHoneycombsFromHivesBounty(Level level, BlockPos pos, Player player) {
		ItemStack goldenComb = getBestFullyChargedGoldenHoneyCombWithHivesBounty(player);
		int hivesBountyLevel = GoldenHoneyCombItem.getHivesBountyLevel(goldenComb);

		if (hivesBountyLevel <= 0) {
			return;
		}

		if (!rollHivesBountyChance(level, hivesBountyLevel)) {
			return;
		}

		Block.popResource(level, pos, new ItemStack(Items.HONEYCOMB, hivesBountyLevel));
	}

	private static void tryRefundBottleFromHivesBounty(Level level, BlockPos pos, Player player) {
		ItemStack goldenComb = getBestFullyChargedGoldenHoneyCombWithHivesBounty(player);
		int hivesBountyLevel = GoldenHoneyCombItem.getHivesBountyLevel(goldenComb);

		if (hivesBountyLevel < 2) {
			return;
		}

		if (player.getAbilities().instabuild) {
			return;
		}

		if (!rollHivesBountyChance(level, hivesBountyLevel)) {
			return;
		}

		ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
		if (!player.getInventory().add(bottle)) {
			Block.popResource(level, pos, bottle);
		}
	}

	private static boolean rollHivesBountyChance(Level level, int hivesBountyLevel) {
		float chance = (15.0F + 5.0F * hivesBountyLevel) / 100.0F;
		return level.random.nextFloat() < chance;
	}

	private static int getBestHivesBountyLevel(Player player) {
		return Math.max(
				GoldenHoneyCombItem.getHivesBountyLevel(player.getOffhandItem()),
				GoldenHoneyCombItem.getHivesBountyLevel(player.getMainHandItem())
		);
	}

	private static ItemStack getBestFullyChargedGoldenHoneyCombWithHivesBounty(Player player) {
		ItemStack offhandStack = player.getOffhandItem();
		ItemStack mainHandStack = player.getMainHandItem();

		int offhandLevel = GoldenHoneyCombItem.isFullyCharged(offhandStack) ? GoldenHoneyCombItem.getHivesBountyLevel(offhandStack) : 0;
		int mainHandLevel = GoldenHoneyCombItem.isFullyCharged(mainHandStack) ? GoldenHoneyCombItem.getHivesBountyLevel(mainHandStack) : 0;

		return offhandLevel >= mainHandLevel ? offhandStack : mainHandStack;
	}

	@Redirect(
			method = "useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/ItemInteractionResult;",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/CampfireBlock;isSmokeyPos(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"
			),
			require = 0
	)
	private boolean timothatys_trinkets$goldenHoneyCombActsAsSmoke(
			Level smokeCheckLevel,
			BlockPos smokeCheckPos,
			ItemStack stack,
			BlockState state,
			Level level,
			BlockPos hivePos,
			Player player,
			InteractionHand hand,
			BlockHitResult hitResult
	) {
		if (CampfireBlock.isSmokeyPos(smokeCheckLevel, smokeCheckPos)) {
			return true;
		}

		return player.getOffhandItem().is(TimothatysTrinketsModItems.GOLDEN_HONEY_COMB.get());
	}
}
