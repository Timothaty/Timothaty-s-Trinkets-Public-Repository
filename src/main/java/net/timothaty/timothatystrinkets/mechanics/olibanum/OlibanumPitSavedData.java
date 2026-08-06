package net.timothaty.timothatystrinkets.mechanics.olibanum;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;
import net.timothaty.timothatystrinkets.particle.RitualSmokeParticleOptions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.saveddata.SavedData;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OlibanumPitSavedData extends SavedData {
	public static final long PROCESS_DURATION_TICKS = 7_777L;

	private static final String DATA_NAME = "timothatys_trinkets_olibanum_pits";
	private static final String PROCESSES = "Processes";
	private static final String POT_POS = "PotPos";
	private static final String PROCESS_STATE = "State";
	private static final String FINISH_TIME = "FinishTime";
	private static final String ORIGINAL_STACK = "OriginalStack";
	private static final int CHECK_INTERVAL_TICKS = 20;
	private static final Factory<OlibanumPitSavedData> FACTORY = new Factory<>(
			OlibanumPitSavedData::new,
			OlibanumPitSavedData::load
	);

	private final Map<Long, Process> activeProcesses = new HashMap<>();
	private final Set<Long> pendingStartChecks = new HashSet<>();

	public static OlibanumPitSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	public void requestStartCheck(BlockPos potPos) {
		pendingStartChecks.add(potPos.asLong());
	}

	public boolean isActive(BlockPos potPos) {
		return activeProcesses.containsKey(potPos.asLong());
	}

	public void tick(ServerLevel level) {
		long gameTime = level.getGameTime();
		if (gameTime % CHECK_INTERVAL_TICKS == 0L && !activeProcesses.isEmpty()) {
			checkActiveProcesses(level, gameTime);
		}
		processPendingStarts(level);
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (Map.Entry<Long, Process> entry : activeProcesses.entrySet()) {
			CompoundTag processTag = new CompoundTag();
			processTag.putLong(POT_POS, entry.getKey());
			Process process = entry.getValue();
			processTag.putString(PROCESS_STATE, process.state().name());
			if (process.state() == ProcessState.PROCESSING) {
				processTag.putLong(FINISH_TIME, process.finishTime());
				processTag.put(ORIGINAL_STACK, process.originalStack().save(registries));
			}
			list.add(processTag);
		}
		tag.put(PROCESSES, list);
		return tag;
	}

	private void checkActiveProcesses(ServerLevel level, long gameTime) {
		List<BlockPos> restartCandidates = new ArrayList<>();
		Iterator<Map.Entry<Long, Process>> iterator = activeProcesses.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Long, Process> entry = iterator.next();
			BlockPos potPos = BlockPos.of(entry.getKey());
			Process process = entry.getValue();
			if (process.state() == ProcessState.SMOULDERING) {
				if (!isValidSmoulderingPit(level, potPos)) {
					iterator.remove();
					setDirty();
					restartCandidates.add(potPos);
				} else {
					spawnSmoulderingSmoke(level, potPos);
				}
				continue;
			}

			if (!OlibanumPitStructure.areRequiredChunksLoaded(level, potPos)) {
				continue;
			}

			DecoratedPotBlockEntity pot = OlibanumPitStructure.getPot(level, potPos);
			ItemStack current = pot == null ? ItemStack.EMPTY : pot.getTheItem();
			if (!matchesOriginal(current, process.originalStack()) || !OlibanumPitStructure.isValid(level, potPos)) {
				iterator.remove();
				setDirty();
				restartCandidates.add(potPos);
				continue;
			}

			if (gameTime >= process.finishTime()) {
				complete(level, potPos, pot, process.originalStack().getCount());
				entry.setValue(Process.smouldering());
				setDirty();
			} else {
				spawnOccasionalEffects(level, potPos);
			}
		}

		for (BlockPos potPos : restartCandidates) {
			tryStart(level, potPos);
		}
	}

	private void processPendingStarts(ServerLevel level) {
		if (pendingStartChecks.isEmpty()) {
			return;
		}
		Set<Long> pending = Set.copyOf(pendingStartChecks);
		pendingStartChecks.clear();
		for (long packedPos : pending) {
			if (!activeProcesses.containsKey(packedPos)) {
				tryStart(level, BlockPos.of(packedPos));
			}
		}
	}

	private void tryStart(ServerLevel level, BlockPos potPos) {
		if (activeProcesses.containsKey(potPos.asLong()) || !OlibanumPitStructure.isValid(level, potPos)) {
			return;
		}

		DecoratedPotBlockEntity pot = OlibanumPitStructure.getPot(level, potPos);
		if (pot == null) {
			return;
		}
		ItemStack stack = pot.getTheItem();
		if (!stack.is(TimothatysTrinketsModItems.AROMATIC_OLIBANUM.get()) || stack.getCount() < 4) {
			return;
		}

		activeProcesses.put(potPos.asLong(), Process.processing(
				level.getGameTime() + PROCESS_DURATION_TICKS,
				stack.copy()
		));
		setDirty();
	}

	private static boolean matchesOriginal(ItemStack current, ItemStack original) {
		return current.getCount() == original.getCount() && ItemStack.isSameItemSameComponents(current, original);
	}

	private static void complete(ServerLevel level, BlockPos potPos, DecoratedPotBlockEntity pot, int resinCount) {
		int output = (resinCount / 4) * 2;
		pot.setTheItem(new ItemStack(TimothatysTrinketsModItems.INCENSE.get(), output));
		pot.setChanged();
		BlockState state = level.getBlockState(potPos);
		level.sendBlockUpdated(potPos, state, state, 3);
		level.gameEvent(null, GameEvent.BLOCK_CHANGE, potPos);
	}

	private static void spawnOccasionalEffects(ServerLevel level, BlockPos potPos) {
		if (level.random.nextInt(6) == 0) {
			level.sendParticles(
					ParticleTypes.SMOKE,
					potPos.getX() + 0.5D,
					potPos.getY() + 2.12D,
					potPos.getZ() + 0.5D,
					1,
					0.07D, 0.025D, 0.07D,
					0.005D
			);
		}
		if (level.random.nextInt(12) == 0) {
			level.playSound(
					null,
					potPos.below(),
					SoundEvents.CAMPFIRE_CRACKLE,
					SoundSource.BLOCKS,
					0.14F,
					0.62F + level.random.nextFloat() * 0.08F
			);
		}
	}

	private static boolean isValidSmoulderingPit(ServerLevel level, BlockPos potPos) {
		if (!level.hasChunkAt(potPos)) {
			return false;
		}
		DecoratedPotBlockEntity pot = OlibanumPitStructure.getPot(level, potPos);
		return pot != null
				&& pot.getTheItem().is(TimothatysTrinketsModItems.INCENSE.get())
				&& OlibanumPitStructure.isValid(level, potPos);
	}

	private static void spawnSmoulderingSmoke(ServerLevel level, BlockPos potPos) {
		RandomSource random = level.getRandom();
		double x = potPos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.18D;
		double y = potPos.getY() + 2.02D;
		double z = potPos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.18D;
		float targetX = (random.nextFloat() - 0.5F) * 0.16F;
		float targetY = 0.5F + random.nextFloat() * 0.3F;
		float targetZ = (random.nextFloat() - 0.5F) * 0.16F;
		level.sendParticles(
				new RitualSmokeParticleOptions(new Vector3f(targetX, targetY, targetZ)),
				x, y, z,
				1 + random.nextInt(2),
				0.025D, 0.008D, 0.025D,
				0.0D
		);
	}

	private static OlibanumPitSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
		OlibanumPitSavedData data = new OlibanumPitSavedData();
		ListTag list = tag.getList(PROCESSES, Tag.TAG_COMPOUND);
		for (int index = 0; index < list.size(); index++) {
			CompoundTag processTag = list.getCompound(index);
			if (!processTag.contains(POT_POS, Tag.TAG_LONG)) {
				continue;
			}
			ProcessState state = readState(processTag);
			if (state == ProcessState.SMOULDERING) {
				data.activeProcesses.put(processTag.getLong(POT_POS), Process.smouldering());
				continue;
			}
			if (!processTag.contains(FINISH_TIME, Tag.TAG_LONG) || !processTag.contains(ORIGINAL_STACK)) {
				continue;
			}
			ItemStack original = ItemStack.parse(registries, processTag.get(ORIGINAL_STACK)).orElse(ItemStack.EMPTY);
			if (original.isEmpty()) {
				continue;
			}
			data.activeProcesses.put(
					processTag.getLong(POT_POS),
					Process.processing(processTag.getLong(FINISH_TIME), original)
			);
		}
		return data;
	}

	private static ProcessState readState(CompoundTag tag) {
		if (!tag.contains(PROCESS_STATE, Tag.TAG_STRING)) {
			return ProcessState.PROCESSING;
		}
		try {
			return ProcessState.valueOf(tag.getString(PROCESS_STATE));
		} catch (IllegalArgumentException ignored) {
			return ProcessState.PROCESSING;
		}
	}

	private enum ProcessState {
		PROCESSING,
		SMOULDERING
	}

	private record Process(ProcessState state, long finishTime, ItemStack originalStack) {
		private static Process processing(long finishTime, ItemStack originalStack) {
			return new Process(ProcessState.PROCESSING, finishTime, originalStack);
		}

		private static Process smouldering() {
			return new Process(ProcessState.SMOULDERING, 0L, ItemStack.EMPTY);
		}
	}
}
