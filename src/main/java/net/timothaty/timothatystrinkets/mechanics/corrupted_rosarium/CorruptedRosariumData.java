package net.timothaty.timothatystrinkets.mechanics.corrupted_rosarium;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class CorruptedRosariumData {
	public static final int SLOT_COUNT = 2;
	public static final TagKey<Item> BEADS_TAG = TagKey.create(
			Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "unholy_bead")
	);
	public static final TagKey<Item> HOLY_BEADS_TAG = TagKey.create(
			Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "holy_beads")
	);

	private static final String BEADS_KEY = "CorruptedRosariumBeads";

	private CorruptedRosariumData() {
	}

	public static boolean canInsert(ItemStack rosarium, ItemStack beadStack) {
		return !beadStack.isEmpty()
				&& beadStack.is(BEADS_TAG)
				&& !beadStack.is(HOLY_BEADS_TAG)
				&& getBeadIds(rosarium).size() < SLOT_COUNT;
	}

	public static boolean insertOne(ItemStack rosarium, ItemStack beadStack) {
		if (!canInsert(rosarium, beadStack))
			return false;

		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(beadStack.getItem());
		if (!addBeadId(rosarium, itemId))
			return false;

		beadStack.shrink(1);
		return true;
	}

	public static boolean addBeadId(ItemStack rosarium, ResourceLocation itemId) {
		if (itemId == null || getBeadIds(rosarium).size() >= SLOT_COUNT)
			return false;

		CompoundTag root = getRootTag(rosarium);
		ListTag beads = root.getList(BEADS_KEY, Tag.TAG_STRING);
		beads.add(StringTag.valueOf(itemId.toString()));
		root.put(BEADS_KEY, beads);
		CustomData.set(DataComponents.CUSTOM_DATA, rosarium, root);
		return true;
	}

	public static ItemStack removeLast(ItemStack rosarium) {
		List<ResourceLocation> beadIds = getBeadIds(rosarium);
		if (beadIds.isEmpty())
			return ItemStack.EMPTY;

		ResourceLocation removedId = beadIds.remove(beadIds.size() - 1);
		writeBeadIds(rosarium, beadIds);
		return createStack(removedId);
	}

	public static List<ResourceLocation> getBeadIds(ItemStack rosarium) {
		CompoundTag root = rosarium.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		ListTag beads = root.getList(BEADS_KEY, Tag.TAG_STRING);
		List<ResourceLocation> result = new ArrayList<>(SLOT_COUNT);

		for (int index = 0; index < beads.size() && result.size() < SLOT_COUNT; index++) {
			ResourceLocation itemId = ResourceLocation.tryParse(beads.getString(index));
			if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId))
				result.add(itemId);
		}

		return result;
	}

	public static List<ItemStack> getBeadStacks(ItemStack rosarium) {
		return getBeadIds(rosarium).stream()
				.map(CorruptedRosariumData::createStack)
				.filter(stack -> !stack.isEmpty())
				.toList();
	}

	public static Set<CorruptedRosariumBead> getUniqueKnownBeads(ItemStack rosarium) {
		Set<CorruptedRosariumBead> beads = new LinkedHashSet<>();
		for (ResourceLocation itemId : getBeadIds(rosarium)) {
			CorruptedRosariumBead bead = CorruptedRosariumBead.byItemIdOrNull(itemId);
			if (bead != null)
				beads.add(bead);
		}
		return beads;
	}

	@SuppressWarnings("deprecation")
	public static int getKnownBeadMask(ItemStack rosarium) {
		if (rosarium == null || rosarium.isEmpty())
			return 0;

		CompoundTag root = rosarium.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
		ListTag beads = root.getList(BEADS_KEY, Tag.TAG_STRING);
		int mask = 0;
		int acceptedEntries = 0;
		for (int index = 0; index < beads.size() && acceptedEntries < SLOT_COUNT; index++) {
			String serializedItemId = beads.getString(index);
			ResourceLocation itemId = ResourceLocation.tryParse(serializedItemId);
			if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId))
				continue;

			acceptedEntries++;
			CorruptedRosariumBead bead = CorruptedRosariumBead.bySerializedItemIdOrNull(serializedItemId);
			if (bead != null)
				mask |= bead.bit();
		}
		return mask;
	}

	public static Optional<CorruptedRosariumCombination> getCombination(ItemStack rosarium) {
		return CorruptedRosariumCombination.fromMask(getKnownBeadMask(rosarium));
	}

	public static void clear(ItemStack rosarium) {
		writeBeadIds(rosarium, List.of());
	}

	private static ItemStack createStack(ResourceLocation itemId) {
		Optional<Item> item = BuiltInRegistries.ITEM.getOptional(itemId);
		return item.filter(value -> value != Items.AIR)
				.map(ItemStack::new)
				.orElse(ItemStack.EMPTY);
	}

	private static CompoundTag getRootTag(ItemStack rosarium) {
		return rosarium.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	private static void writeBeadIds(ItemStack rosarium, List<ResourceLocation> beadIds) {
		CompoundTag root = getRootTag(rosarium);
		if (beadIds.isEmpty()) {
			root.remove(BEADS_KEY);
		} else {
			ListTag beads = new ListTag();
			for (ResourceLocation itemId : beadIds)
				beads.add(StringTag.valueOf(itemId.toString()));
			root.put(BEADS_KEY, beads);
		}
		CustomData.set(DataComponents.CUSTOM_DATA, rosarium, root);
	}
}
