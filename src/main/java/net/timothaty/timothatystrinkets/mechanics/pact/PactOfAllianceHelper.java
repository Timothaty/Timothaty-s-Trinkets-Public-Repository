package net.timothaty.timothatystrinkets.mechanics.pact;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PactOfAllianceHelper {
	public static final int MAX_MEMBERS = 8;

	private static final String MEMBERS_KEY = "ttr_pact_of_alliance_members";
	private static final String MEMBER_UUID_KEY = "uuid";
	private static final String MEMBER_NAME_KEY = "name";

	private PactOfAllianceHelper() {
	}

	public static boolean isPactStack(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.is(TimothatysTrinketsModItems.PACT_OF_ALLIANCE.get());
	}

	public static AddMemberResult addOrRefreshMember(ItemStack stack, Player member) {
		if (!isPactStack(stack) || member == null)
			return AddMemberResult.INVALID;

		CompoundTag tag = getCustomTag(stack);
		ListTag members = tag.getList(MEMBERS_KEY, Tag.TAG_COMPOUND);
		UUID memberUuid = member.getUUID();
		String memberName = member.getGameProfile().getName();

		for (int index = 0; index < members.size(); index++) {
			CompoundTag entry = members.getCompound(index);
			if (entry.hasUUID(MEMBER_UUID_KEY) && memberUuid.equals(entry.getUUID(MEMBER_UUID_KEY))) {
				entry.putString(MEMBER_NAME_KEY, memberName);
				tag.put(MEMBERS_KEY, members);
				setCustomTag(stack, tag);
				return AddMemberResult.UPDATED;
			}
		}

		if (members.size() >= MAX_MEMBERS)
			return AddMemberResult.FULL;

		CompoundTag entry = new CompoundTag();
		entry.putUUID(MEMBER_UUID_KEY, memberUuid);
		entry.putString(MEMBER_NAME_KEY, memberName);
		members.add(entry);
		tag.put(MEMBERS_KEY, members);
		setCustomTag(stack, tag);
		return AddMemberResult.ADDED;
	}

	public static boolean removeMember(ItemStack stack, UUID memberUuid) {
		if (!isPactStack(stack) || memberUuid == null)
			return false;

		CompoundTag tag = getCustomTag(stack);
		ListTag members = tag.getList(MEMBERS_KEY, Tag.TAG_COMPOUND);
		boolean removed = false;

		for (int index = members.size() - 1; index >= 0; index--) {
			CompoundTag entry = members.getCompound(index);
			if (!entry.hasUUID(MEMBER_UUID_KEY) || memberUuid.equals(entry.getUUID(MEMBER_UUID_KEY))) {
				members.remove(index);
				removed = true;
			}
		}

		if (!removed)
			return false;

		if (members.isEmpty()) {
			tag.remove(MEMBERS_KEY);
		} else {
			tag.put(MEMBERS_KEY, members);
		}
		setCustomTag(stack, tag);
		return true;
	}

	public static boolean containsMember(ItemStack stack, UUID memberUuid) {
		if (!isPactStack(stack) || memberUuid == null)
			return false;

		for (PactMember member : getMembers(stack)) {
			if (memberUuid.equals(member.uuid()))
				return true;
		}
		return false;
	}

	public static int getMemberCount(ItemStack stack) {
		if (!isPactStack(stack))
			return 0;
		return Math.min(MAX_MEMBERS, getMembersTag(stack).size());
	}

	public static List<PactMember> getMembers(ItemStack stack) {
		List<PactMember> result = new ArrayList<>();
		if (!isPactStack(stack))
			return result;

		ListTag members = getMembersTag(stack);
		for (int index = 0; index < members.size(); index++) {
			CompoundTag entry = members.getCompound(index);
			if (!entry.hasUUID(MEMBER_UUID_KEY))
				continue;

			UUID uuid = entry.getUUID(MEMBER_UUID_KEY);
			String name = entry.getString(MEMBER_NAME_KEY);
			if (name == null || name.isBlank()) {
				name = uuid.toString();
			}
			result.add(new PactMember(uuid, name));
		}
		return result;
	}

	public static boolean areAllied(Player relicOwner, LivingEntity possibleTarget) {
		if (relicOwner == null || !(possibleTarget instanceof Player targetPlayer))
			return false;
		if (relicOwner == targetPlayer)
			return false;

		return playerHasPactMember(relicOwner, targetPlayer.getUUID())
				|| playerHasPactMember(targetPlayer, relicOwner.getUUID());
	}

	public static boolean hasMember(Player relicOwner, UUID possibleMemberUuid) {
		return playerHasPactMember(relicOwner, possibleMemberUuid);
	}

	private static boolean playerHasPactMember(Player holder, UUID memberUuid) {
		if (holder == null || memberUuid == null)
			return false;

		Inventory inventory = holder.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (containsMember(inventory.getItem(slot), memberUuid))
				return true;
		}
		return false;
	}

	private static ListTag getMembersTag(ItemStack stack) {
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag.getList(MEMBERS_KEY, Tag.TAG_COMPOUND);
	}

	private static CompoundTag getCustomTag(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	private static void setCustomTag(ItemStack stack, CompoundTag tag) {
		if (tag.isEmpty()) {
			stack.remove(DataComponents.CUSTOM_DATA);
		} else {
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
	}

	public enum AddMemberResult {
		ADDED,
		UPDATED,
		FULL,
		INVALID
	}

	public record PactMember(UUID uuid, String name) {
	}
}
