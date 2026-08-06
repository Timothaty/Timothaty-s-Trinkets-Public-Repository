package net.timothaty.timothatystrinkets.mechanics.striker_acquisition;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;
import java.util.UUID;

public final class StrikerCommissionData {
	private static final String STAGE_KEY = "ttr_striker_commission_stage";
	private static final String RECIPIENT_KEY = "ttr_striker_recipient_uuid";
	private static final String RAID_ID_KEY = "ttr_striker_raid_id";
	private static final String RAID_CENTER_KEY = "ttr_striker_raid_center";
	private static final String RAID_DIMENSION_KEY = "ttr_striker_raid_dimension";
	private static final String NEXT_RECIPIENT_SCAN_KEY = "ttr_striker_next_recipient_scan";
	private static final String VISUAL_ACTIVE_KEY = "ttr_striker_visual_active";
	private static final String PREVIOUS_MAIN_HAND_KEY = "ttr_striker_previous_main_hand";

	private StrikerCommissionData() {
	}

	public static StrikerCommissionStage getStage(Villager villager) {
		return villager == null
				? StrikerCommissionStage.NONE
				: StrikerCommissionStage.fromStoredName(villager.getPersistentData().getString(STAGE_KEY));
	}

	public static void setStage(Villager villager, StrikerCommissionStage stage) {
		if (villager == null)
			return;
		villager.getPersistentData().putString(STAGE_KEY, stage == null ? StrikerCommissionStage.NONE.name() : stage.name());
	}

	public static Optional<UUID> getRecipientId(Villager villager) {
		if (villager == null || !villager.getPersistentData().hasUUID(RECIPIENT_KEY))
			return Optional.empty();
		return Optional.of(villager.getPersistentData().getUUID(RECIPIENT_KEY));
	}

	public static long getNextRecipientScan(Villager villager) {
		return villager == null ? 0L : villager.getPersistentData().getLong(NEXT_RECIPIENT_SCAN_KEY);
	}

	public static void setNextRecipientScan(Villager villager, long gameTime) {
		if (villager != null)
			villager.getPersistentData().putLong(NEXT_RECIPIENT_SCAN_KEY, gameTime);
	}

	public static Optional<RaidIdentity> getRaidIdentity(Villager villager) {
		if (villager == null)
			return Optional.empty();
		CompoundTag data = villager.getPersistentData();
		if (!data.contains(RAID_ID_KEY, Tag.TAG_INT)
				|| !data.contains(RAID_CENTER_KEY, Tag.TAG_LONG)
				|| !data.contains(RAID_DIMENSION_KEY, Tag.TAG_STRING))
			return Optional.empty();
		return Optional.of(new RaidIdentity(
				data.getInt(RAID_ID_KEY),
				BlockPos.of(data.getLong(RAID_CENTER_KEY)),
				data.getString(RAID_DIMENSION_KEY)
		));
	}

	public static boolean assignRaidCommission(ServerLevel level, Villager villager, UUID recipientId, Raid raid) {
		if (level == null || villager == null || recipientId == null || raid == null
				|| getStage(villager).isActive()
				|| villager.getPersistentData().getBoolean(VISUAL_ACTIVE_KEY))
			return false;

		clearStoredCommission(villager);
		CompoundTag data = villager.getPersistentData();
		data.putUUID(RECIPIENT_KEY, recipientId);
		data.putInt(RAID_ID_KEY, raid.getId());
		data.putLong(RAID_CENTER_KEY, raid.getCenter().asLong());
		data.putString(RAID_DIMENSION_KEY, level.dimension().location().toString());
		setStage(villager, StrikerCommissionStage.WAITING_FOR_RAID_VICTORY);
		return true;
	}

	public static boolean assignDebugCommission(Villager villager, UUID recipientId) {
		if (villager == null || recipientId == null || getStage(villager).isActive()
				|| villager.getPersistentData().getBoolean(VISUAL_ACTIVE_KEY))
			return false;

		clearStoredCommission(villager);
		villager.getPersistentData().putUUID(RECIPIENT_KEY, recipientId);
		setStage(villager, StrikerCommissionStage.FORGING_PENDING);
		return true;
	}

	public static boolean isQualifiedWeaponsmith(Villager villager) {
		return villager != null
				&& villager.isAlive()
				&& !villager.isRemoved()
				&& !villager.isBaby()
				&& villager.getVillagerData().getProfession() == VillagerProfession.WEAPONSMITH
				&& villager.getVillagerData().getLevel() >= 3;
	}

	public static boolean isEligibleForAssignment(ServerLevel level, Villager villager) {
		return isQualifiedWeaponsmith(villager)
				&& getStage(villager) == StrikerCommissionStage.NONE
				&& !villager.getPersistentData().getBoolean(VISUAL_ACTIVE_KEY)
				&& getLoadedGrindstone(level, villager).isPresent();
	}

	public static Optional<GlobalPos> getJobSite(Villager villager) {
		if (villager == null)
			return Optional.empty();
		return villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
	}

	public static Optional<BlockPos> getLoadedGrindstone(ServerLevel level, Villager villager) {
		if (level == null)
			return Optional.empty();
		return getJobSite(villager)
				.filter(jobSite -> jobSite.dimension().equals(level.dimension()))
				.map(GlobalPos::pos)
				.filter(level::hasChunkAt)
				.filter(pos -> level.getBlockState(pos).is(Blocks.GRINDSTONE));
	}

	public static boolean jobSiteMatches(ServerLevel level, Villager villager, BlockPos expectedPos) {
		return expectedPos != null
				&& getJobSite(villager)
						.filter(jobSite -> jobSite.dimension().equals(level.dimension()))
						.map(GlobalPos::pos)
						.filter(expectedPos::equals)
						.filter(level::hasChunkAt)
						.filter(pos -> level.getBlockState(pos).is(Blocks.GRINDSTONE))
						.isPresent();
	}

	public static boolean raidMatches(ServerLevel level, Villager villager, Raid raid) {
		return raid != null && getRaidIdentity(villager)
				.map(identity -> identity.id() == raid.getId()
						&& identity.dimension().equals(level.dimension().location().toString()))
				.orElse(false);
	}

	public static void normalizeOnJoin(ServerLevel level, Villager villager) {
		StrikerCommissionStage stage = getStage(villager);
		if (!stage.isActive()) {
			clearVisualItem(villager);
			return;
		}
		if (!isQualifiedWeaponsmith(villager)) {
			clearCommission(level, villager);
			return;
		}

		switch (stage) {
			case WALKING_TO_WORKSTATION, FORGING -> {
				setStage(villager, StrikerCommissionStage.FORGING_PENDING);
				clearVisualItem(villager);
			}
			case DELIVERING -> {
				setStage(villager, StrikerCommissionStage.DELIVERY_PENDING);
				ensureDeliveryVisual(villager);
				StrikerForgingDeliveryGoal.scheduleNextRecipientScan(level, villager);
			}
			case DELIVERY_PENDING -> {
				ensureDeliveryVisual(villager);
				StrikerForgingDeliveryGoal.scheduleNextRecipientScan(level, villager);
			}
			case WAITING_FOR_RAID_VICTORY, FORGING_PENDING -> clearVisualItem(villager);
			default -> {
			}
		}
	}

	public static void showVisualItem(Villager villager, ItemStack stack) {
		if (villager == null || stack == null || villager.getTradingPlayer() != null)
			return;

		CompoundTag data = villager.getPersistentData();
		if (!data.getBoolean(VISUAL_ACTIVE_KEY)) {
			data.put(PREVIOUS_MAIN_HAND_KEY, villager.getMainHandItem().saveOptional(villager.registryAccess()));
			data.putBoolean(VISUAL_ACTIVE_KEY, true);
		}
		villager.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
		villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	public static void ensureDeliveryVisual(Villager villager) {
		showVisualItem(villager, new ItemStack(TimothatysTrinketsModItems.STRIKER_OF_THE_MORNING_STAR.get()));
	}

	public static void clearVisualItem(Villager villager) {
		if (villager == null)
			return;
		CompoundTag data = villager.getPersistentData();
		if (!data.getBoolean(VISUAL_ACTIVE_KEY))
			return;
		if (villager.getTradingPlayer() != null)
			return;

		ItemStack previous = data.contains(PREVIOUS_MAIN_HAND_KEY, Tag.TAG_COMPOUND)
				? ItemStack.parseOptional(villager.registryAccess(), data.getCompound(PREVIOUS_MAIN_HAND_KEY))
				: ItemStack.EMPTY;
		villager.setItemSlot(EquipmentSlot.MAINHAND, previous);
		villager.setDropChance(EquipmentSlot.MAINHAND, Mob.DEFAULT_EQUIPMENT_DROP_CHANCE);
		data.remove(VISUAL_ACTIVE_KEY);
		data.remove(PREVIOUS_MAIN_HAND_KEY);
	}

	public static void clearCommission(ServerLevel level, Villager villager) {
		if (villager == null)
			return;
		clearVisualItem(villager);
		clearStoredCommission(villager);
		villager.getNavigation().stop();
		villager.getBrain().eraseMemory(MemoryModuleType.PATH);
		villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
		if (level != null && !isSafetyActivityActive(villager))
			villager.getBrain().updateActivityFromSchedule(level.getDayTime(), level.getGameTime());
	}

	private static void clearStoredCommission(Villager villager) {
		CompoundTag data = villager.getPersistentData();
		data.putString(STAGE_KEY, StrikerCommissionStage.NONE.name());
		data.remove(RECIPIENT_KEY);
		data.remove(RAID_ID_KEY);
		data.remove(RAID_CENTER_KEY);
		data.remove(RAID_DIMENSION_KEY);
		data.remove(NEXT_RECIPIENT_SCAN_KEY);
	}

	private static boolean isSafetyActivityActive(Villager villager) {
		return villager.getBrain().isActive(Activity.PANIC)
				|| villager.getBrain().isActive(Activity.HIDE)
				|| villager.getBrain().isActive(Activity.RAID)
				|| villager.getBrain().isActive(Activity.PRE_RAID)
				|| villager.getBrain().isActive(Activity.REST)
				|| villager.getBrain().isActive(Activity.CELEBRATE);
	}

	public record RaidIdentity(int id, BlockPos center, String dimension) {
	}
}
