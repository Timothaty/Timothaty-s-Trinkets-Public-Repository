package net.timothaty.timothatystrinkets.mechanics.cleric_quests;

import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityStage;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament.SacramentStage;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClericQuestProgress {
	private static final String HUMILITY_COMPLETED = "HumilityCompleted";
	private static final String HUMILITY_STAGE = "HumilityStage";
	private static final String HUMILITY_CLERIC = "HumilityCleric";
	private static final String HUMILITY_DIMENSION = "HumilityDimension";
	private static final String HUMILITY_DEEDS = "HumilityDeeds";
	private static final String SACRAMENT_COMPLETED = "SacramentCompleted";
	private static final String SACRAMENT_STAGE = "SacramentStage";
	private static final String SACRAMENT_CLERIC = "SacramentCleric";
	private static final String SACRAMENT_DIMENSION = "SacramentDimension";
	private static final String SACRAMENT_OFFERINGS = "SacramentOfferings";
	private static final String SACRAMENT_TARGETS = "SacramentTargets";
	private static final String SACRAMENT_KILLS = "SacramentKills";
	private static final String FASTING_SECONDS = "FastingSeconds";
	private static final String FASTING_HAS_STARTED = "FastingHasStarted";
	private static final String DESERT_EXIT_GRACE = "DesertExitGraceSeconds";

	private boolean humilityCompleted;
	private HumilityStage humilityStage = HumilityStage.NONE;
	private UUID humilityClericId;
	private ResourceKey<Level> humilityClericDimension;
	private int humilityDeedMask;

	private boolean sacramentCompleted;
	private SacramentStage sacramentStage = SacramentStage.NONE;
	private UUID sacramentClericId;
	private ResourceKey<Level> sacramentClericDimension;
	private int sacramentOfferingMask;
	private final List<ResourceLocation> sacramentTargets = new ArrayList<>(3);
	private int sacramentKilledMask;
	private int fastingSeconds;
	private boolean fastingHasStarted;
	private int desertExitGraceSeconds;

	public static ClericQuestProgress load(CompoundTag tag) {
		ClericQuestProgress progress = new ClericQuestProgress();
		progress.humilityCompleted = tag.getBoolean(HUMILITY_COMPLETED);
		progress.humilityStage = HumilityStage.byName(tag.getString(HUMILITY_STAGE));
		progress.humilityClericId = tag.hasUUID(HUMILITY_CLERIC) ? tag.getUUID(HUMILITY_CLERIC) : null;
		progress.humilityClericDimension = readDimension(tag, HUMILITY_DIMENSION);
		progress.humilityDeedMask = tag.getInt(HUMILITY_DEEDS);
		progress.sacramentCompleted = tag.getBoolean(SACRAMENT_COMPLETED);
		progress.sacramentStage = SacramentStage.byName(tag.getString(SACRAMENT_STAGE));
		progress.sacramentClericId = tag.hasUUID(SACRAMENT_CLERIC) ? tag.getUUID(SACRAMENT_CLERIC) : null;
		progress.sacramentClericDimension = readDimension(tag, SACRAMENT_DIMENSION);
		progress.sacramentOfferingMask = tag.getInt(SACRAMENT_OFFERINGS);
		ListTag targets = tag.getList(SACRAMENT_TARGETS, Tag.TAG_STRING);
		for (int index = 0; index < targets.size() && progress.sacramentTargets.size() < 3; index++) {
			ResourceLocation id = ResourceLocation.tryParse(targets.getString(index));
			if (id != null && !progress.sacramentTargets.contains(id))
				progress.sacramentTargets.add(id);
		}
		progress.sacramentKilledMask = tag.getInt(SACRAMENT_KILLS);
		progress.fastingSeconds = Math.max(0, tag.getInt(FASTING_SECONDS));
		progress.desertExitGraceSeconds = Math.max(0, tag.getInt(DESERT_EXIT_GRACE));
		progress.fastingHasStarted = tag.contains(FASTING_HAS_STARTED, Tag.TAG_BYTE)
			? tag.getBoolean(FASTING_HAS_STARTED)
			: progress.fastingSeconds > 0 || progress.desertExitGraceSeconds > 0;
		progress.repairInvariants();
		return progress;
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(HUMILITY_COMPLETED, humilityCompleted);
		tag.putString(HUMILITY_STAGE, humilityStage.name());
		writeBinding(tag, HUMILITY_CLERIC, HUMILITY_DIMENSION, humilityClericId, humilityClericDimension);
		tag.putInt(HUMILITY_DEEDS, humilityDeedMask);
		tag.putBoolean(SACRAMENT_COMPLETED, sacramentCompleted);
		tag.putString(SACRAMENT_STAGE, sacramentStage.name());
		writeBinding(tag, SACRAMENT_CLERIC, SACRAMENT_DIMENSION, sacramentClericId, sacramentClericDimension);
		tag.putInt(SACRAMENT_OFFERINGS, sacramentOfferingMask);
		ListTag targets = new ListTag();
		for (ResourceLocation target : sacramentTargets)
			targets.add(StringTag.valueOf(target.toString()));
		tag.put(SACRAMENT_TARGETS, targets);
		tag.putInt(SACRAMENT_KILLS, sacramentKilledMask);
		tag.putInt(FASTING_SECONDS, fastingSeconds);
		tag.putBoolean(FASTING_HAS_STARTED, fastingHasStarted);
		tag.putInt(DESERT_EXIT_GRACE, desertExitGraceSeconds);
		return tag;
	}

	private void repairInvariants() {
		if (humilityCompleted)
			humilityStage = HumilityStage.COMPLETED;
		if (sacramentCompleted)
			sacramentStage = SacramentStage.COMPLETED;
		if (humilityStage == HumilityStage.NONE || humilityStage == HumilityStage.COMPLETED) {
			humilityClericId = null;
			humilityClericDimension = null;
		}
		if (sacramentStage == SacramentStage.NONE || sacramentStage == SacramentStage.COMPLETED) {
			sacramentClericId = null;
			sacramentClericDimension = null;
		}
		if (sacramentTargets.size() < 3)
			sacramentKilledMask &= (1 << sacramentTargets.size()) - 1;
		if (sacramentStage != SacramentStage.FAST_ACTIVE)
			fastingHasStarted = false;
	}

	private static ResourceKey<Level> readDimension(CompoundTag tag, String key) {
		ResourceLocation id = ResourceLocation.tryParse(tag.getString(key));
		return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
	}

	private static void writeBinding(CompoundTag tag, String uuidKey, String dimensionKey, UUID uuid, ResourceKey<Level> dimension) {
		if (uuid != null)
			tag.putUUID(uuidKey, uuid);
		if (dimension != null)
			tag.putString(dimensionKey, dimension.location().toString());
	}

	public void beginHumility(UUID clericId, ResourceKey<Level> dimension) {
		humilityStage = HumilityStage.DEEDS_ACTIVE;
		humilityClericId = clericId;
		humilityClericDimension = dimension;
		humilityDeedMask = 0;
	}

	public void resetIncompleteHumility() {
		if (humilityCompleted)
			return;
		humilityStage = HumilityStage.NONE;
		humilityClericId = null;
		humilityClericDimension = null;
		humilityDeedMask = 0;
	}

	public void completeHumility() {
		humilityCompleted = true;
		humilityStage = HumilityStage.COMPLETED;
		humilityClericId = null;
		humilityClericDimension = null;
	}

	public void beginSacrament(UUID clericId, ResourceKey<Level> dimension) {
		sacramentStage = SacramentStage.OFFERINGS;
		sacramentClericId = clericId;
		sacramentClericDimension = dimension;
		sacramentOfferingMask = 0;
		sacramentTargets.clear();
		sacramentKilledMask = 0;
		fastingSeconds = 0;
		fastingHasStarted = false;
		desertExitGraceSeconds = 0;
	}

	public void resetIncompleteSacrament() {
		if (sacramentCompleted)
			return;
		sacramentStage = SacramentStage.NONE;
		sacramentClericId = null;
		sacramentClericDimension = null;
		sacramentOfferingMask = 0;
		sacramentTargets.clear();
		sacramentKilledMask = 0;
		fastingSeconds = 0;
		fastingHasStarted = false;
		desertExitGraceSeconds = 0;
	}

	public void completeSacrament() {
		sacramentCompleted = true;
		sacramentStage = SacramentStage.COMPLETED;
		sacramentClericId = null;
		sacramentClericDimension = null;
		fastingSeconds = 0;
		fastingHasStarted = false;
		desertExitGraceSeconds = 0;
	}

	public boolean humilityCompleted() { return humilityCompleted; }
	public HumilityStage humilityStage() { return humilityStage; }
	public void setHumilityStage(HumilityStage stage) { humilityStage = stage; }
	public UUID humilityClericId() { return humilityClericId; }
	public ResourceKey<Level> humilityClericDimension() { return humilityClericDimension; }
	public int humilityDeedMask() { return humilityDeedMask; }
	public void setHumilityDeedMask(int mask) { humilityDeedMask = mask; }
	public boolean sacramentCompleted() { return sacramentCompleted; }
	public SacramentStage sacramentStage() { return sacramentStage; }
	public void setSacramentStage(SacramentStage stage) { sacramentStage = stage; }
	public UUID sacramentClericId() { return sacramentClericId; }
	public ResourceKey<Level> sacramentClericDimension() { return sacramentClericDimension; }
	public int sacramentOfferingMask() { return sacramentOfferingMask; }
	public void setSacramentOfferingMask(int mask) { sacramentOfferingMask = mask; }
	public List<ResourceLocation> sacramentTargets() { return List.copyOf(sacramentTargets); }
	public void setSacramentTargets(List<ResourceLocation> targets) {
		sacramentTargets.clear();
		for (ResourceLocation target : targets) {
			if (target != null && !sacramentTargets.contains(target) && sacramentTargets.size() < 3)
				sacramentTargets.add(target);
		}
	}
	public int sacramentKilledMask() { return sacramentKilledMask; }
	public void setSacramentKilledMask(int mask) { sacramentKilledMask = mask; }
	public int fastingSeconds() { return fastingSeconds; }
	public void setFastingSeconds(int seconds) { fastingSeconds = Math.max(0, seconds); }
	public boolean fastingHasStarted() { return fastingHasStarted; }
	public void setFastingHasStarted(boolean started) { fastingHasStarted = started; }
	public int desertExitGraceSeconds() { return desertExitGraceSeconds; }
	public void setDesertExitGraceSeconds(int seconds) { desertExitGraceSeconds = Math.max(0, seconds); }

	public UUID activeClericId() {
		if (humilityStage != HumilityStage.NONE && humilityStage != HumilityStage.COMPLETED)
			return humilityClericId;
		if (sacramentStage != SacramentStage.NONE && sacramentStage != SacramentStage.COMPLETED)
			return sacramentClericId;
		return null;
	}

	public ResourceKey<Level> activeClericDimension() {
		if (humilityStage != HumilityStage.NONE && humilityStage != HumilityStage.COMPLETED)
			return humilityClericDimension;
		if (sacramentStage != SacramentStage.NONE && sacramentStage != SacramentStage.COMPLETED)
			return sacramentClericDimension;
		return null;
	}
}
