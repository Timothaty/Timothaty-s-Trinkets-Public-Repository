package net.timothaty.timothatystrinkets.util;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.block.entity.DamnationAltarBlockEntity;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModBlocks;
import net.timothaty.timothatystrinkets.mechanics.debtlord.DebtlordSummonManager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.AABB;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class DamnationAltarOfferDisplayHandler {
	private static final String TAG_OFFER = "tt_damnation_offer";
	private static final String TAG_ALTAR_POS = "tt_damnation_offer_pos";
	private static final double LEGACY_DISPLAY_Y = 1.55D;
	private static final DustParticleOptions OFFER_SPAWN_DUST =
			new DustParticleOptions(new Vector3f(0.70F, 0.20F, 0.90F), 1.0F);

	private DamnationAltarOfferDisplayHandler() {
	}

	public static boolean hasOffer(ServerLevel level, BlockPos altarPos) {
		return level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar
				&& altar.hasExternalOffer();
	}

	public static ItemStack createOfferFromLootTable(ServerLevel level, BlockPos altarPos, ResourceLocation lootId) {
		if (!isAltarUsable(level, altarPos)) return ItemStack.EMPTY;

		ItemStack rolled = rollFirstNonEmpty(level, lootId);
		if (rolled.isEmpty()) return ItemStack.EMPTY;
		if (!(level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar)) return ItemStack.EMPTY;
		if (!altar.trySetExternalOffer(rolled)) return ItemStack.EMPTY;

		spawnOfferSpawnDust(level, altarPos);
		return rolled;
	}

	public static boolean tryCreateOfferFromLootTable(ServerLevel level, BlockPos altarPos, ResourceLocation lootId) {
		return !createOfferFromLootTable(level, altarPos, lootId).isEmpty();
	}

	public static boolean tryCreateOfferFromStack(ServerLevel level, BlockPos altarPos, ItemStack stack) {
		if (stack == null || stack.isEmpty() || !isAltarUsable(level, altarPos)) return false;
		if (!(level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar)) return false;
		if (!altar.trySetExternalOffer(stack)) return false;

		spawnOfferSpawnDust(level, altarPos);
		return true;
	}

	public static boolean tryTakeOffer(ServerLevel level, BlockPos altarPos, ServerPlayer player) {
		if (!(level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar)) return false;
		if (!isAltarAccessible(level, altarPos, altar)) return false;
		ItemStack stack = altar.takeExternalOffer();
		if (stack.isEmpty()) return false;

		if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false);
		level.playSound(
				null,
				altarPos.getX() + 0.5D,
				altarPos.getY() + 0.5D,
				altarPos.getZ() + 0.5D,
				SoundEvents.ARMOR_EQUIP_LEATHER,
				SoundSource.BLOCKS,
				1.0F,
				1.0F
		);
		return true;
	}

	@SubscribeEvent
	public static void onLegacyDisplayJoin(EntityJoinLevelEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level)) return;
		if (!(event.getEntity() instanceof Display.ItemDisplay display) || !isLegacyOffer(display)) return;
		migrateOrRemoveLegacyDisplay(level, display);
	}

	public static void migrateLegacyDisplaysAt(ServerLevel level, BlockPos altarPos) {
		List<Display.ItemDisplay> displays = level.getEntitiesOfClass(
				Display.ItemDisplay.class,
				legacyOfferSearchBox(altarPos),
				DamnationAltarOfferDisplayHandler::isLegacyOffer
		);
		for (Display.ItemDisplay display : displays) {
			CompoundTag data = display.getPersistentData();
			if (!data.contains(TAG_ALTAR_POS, Tag.TAG_LONG)) {
				display.discard();
				continue;
			}
			BlockPos assignedPos = BlockPos.of(data.getLong(TAG_ALTAR_POS));
			if (assignedPos.equals(altarPos)) migrateOrRemoveLegacyDisplay(level, display);
		}
	}

	public static ItemStack recoverLegacyOfferWithoutBlockEntity(ServerLevel level, BlockPos altarPos) {
		ItemStack recovered = ItemStack.EMPTY;
		List<Display.ItemDisplay> displays = level.getEntitiesOfClass(
				Display.ItemDisplay.class,
				legacyOfferSearchBox(altarPos),
				DamnationAltarOfferDisplayHandler::isLegacyOffer
		);
		for (Display.ItemDisplay display : displays) {
			CompoundTag data = display.getPersistentData();
			if (!data.contains(TAG_ALTAR_POS, Tag.TAG_LONG)
					|| !BlockPos.of(data.getLong(TAG_ALTAR_POS)).equals(altarPos)) {
				continue;
			}

			if (recovered.isEmpty()) {
				ItemStack legacyStack = display.getSlot(0).get();
				if (!legacyStack.isEmpty()) recovered = legacyStack.copy();
			}
			display.discard();
		}
		return recovered;
	}

	private static void migrateOrRemoveLegacyDisplay(ServerLevel level, Display.ItemDisplay display) {
		CompoundTag data = display.getPersistentData();
		if (!data.contains(TAG_ALTAR_POS, Tag.TAG_LONG)) {
			display.discard();
			return;
		}

		BlockPos altarPos = BlockPos.of(data.getLong(TAG_ALTAR_POS));
		double expectedX = altarPos.getX() + 0.5D;
		double expectedY = altarPos.getY() + LEGACY_DISPLAY_Y;
		double expectedZ = altarPos.getZ() + 0.5D;
		if (display.distanceToSqr(expectedX, expectedY, expectedZ) > 16.0D) {
			display.discard();
			return;
		}

		if (level.getBlockState(altarPos).getBlock() != TimothatysTrinketsModBlocks.DAMNATION_ALTAR.get()) {
			display.discard();
			return;
		}
		if (!(level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar)) {
			return;
		}

		ItemStack legacyStack = display.getSlot(0).get().copy();
		boolean newlyAdopted = !altar.hasExternalOffer()
				&& !legacyStack.isEmpty()
				&& altar.adoptLegacyExternalOffer(legacyStack);

		display.discard();
		if (newlyAdopted) spawnOfferSpawnDust(level, altarPos);
	}

	private static boolean isLegacyOffer(Display.ItemDisplay display) {
		return display.getPersistentData().getBoolean(TAG_OFFER);
	}

	private static AABB legacyOfferSearchBox(BlockPos altarPos) {
		return new AABB(
				altarPos.getX() - 0.75D, altarPos.getY() + 0.2D, altarPos.getZ() - 0.75D,
				altarPos.getX() + 1.75D, altarPos.getY() + 2.5D, altarPos.getZ() + 1.75D
		);
	}

	private static boolean isAltarUsable(ServerLevel level, BlockPos altarPos) {
		if (level.getBlockState(altarPos).getBlock() != TimothatysTrinketsModBlocks.DAMNATION_ALTAR.get()) return false;
		if (!(level.getBlockEntity(altarPos) instanceof DamnationAltarBlockEntity altar)) return false;
		return isAltarAccessible(level, altarPos, altar) && !altar.hasExternalOffer();
	}

	private static boolean isAltarAccessible(ServerLevel level, BlockPos altarPos, DamnationAltarBlockEntity altar) {
		if (DebtlordSummonManager.isAltarActive(level, altarPos)) return false;
		if (altar.isBusyForExternalRitual()) return false;
		BlockPos above = altarPos.above();
		return !level.getBlockState(above).isCollisionShapeFullBlock(level, above);
	}

	private static void spawnOfferSpawnDust(ServerLevel level, BlockPos altarPos) {
		level.sendParticles(
				OFFER_SPAWN_DUST,
				altarPos.getX() + 0.5D,
				altarPos.getY() + 1.10D,
				altarPos.getZ() + 0.5D,
				28,
				0.35D, 0.18D, 0.35D,
				0.01D
		);
	}

	private static ItemStack rollFirstNonEmpty(ServerLevel level, ResourceLocation lootId) {
		ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, lootId);
		LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
		if (table == LootTable.EMPTY) return ItemStack.EMPTY;

		LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
		List<ItemStack> out = new ArrayList<>();
		long seed = level.getRandom().nextLong();
		table.getRandomItems(params, seed, out::add);
		for (ItemStack stack : out) {
			if (stack != null && !stack.isEmpty()) return stack;
		}
		return ItemStack.EMPTY;
	}
}
