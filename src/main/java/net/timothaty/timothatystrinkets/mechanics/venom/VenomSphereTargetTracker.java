package net.timothaty.timothatystrinkets.mechanics.venom;

import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModMobEffects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class VenomSphereTargetTracker {
	private VenomSphereTargetTracker() {
	}

	public static StackResult recordChargedHit(Player player, LivingEntity target, long now) {
		if (!canUseTargetSlot(player, target, now)) {
			return StackResult.rejectedResult();
		}

		refreshActiveTarget(player, target, now);

		CompoundTag targetData = target.getPersistentData();
		UUID ownerUuid = player.getUUID();
		boolean sameOwner = targetData.hasUUID(VenomSphereData.NBT_STACK_OWNER_UUID)
				&& ownerUuid.equals(targetData.getUUID(VenomSphereData.NBT_STACK_OWNER_UUID));
		boolean stillActive = sameOwner && now <= targetData.getLong(VenomSphereData.NBT_EXPIRE_TICK);

		int stacks = 0;
		int hits = 0;
		if (stillActive) {
			stacks = Mth.clamp(targetData.getInt(VenomSphereData.NBT_STACKS), 0, VenomSphereData.MAX_STACKS);
			hits = Mth.clamp(targetData.getInt(VenomSphereData.NBT_HITS), 0, VenomSphereData.HITS_PER_STACK - 1);
		}

		hits++;
		boolean stackAdded = false;
		if (hits >= VenomSphereData.HITS_PER_STACK) {
			hits = 0;
			if (stacks < VenomSphereData.MAX_STACKS) {
				stacks++;
				stackAdded = true;
			}
		}

		long expireTick = now + VenomSphereData.EFFECT_DURATION_TICKS;
		targetData.putUUID(VenomSphereData.NBT_STACK_OWNER_UUID, ownerUuid);
		targetData.putInt(VenomSphereData.NBT_HITS, hits);
		targetData.putInt(VenomSphereData.NBT_STACKS, stacks);
		targetData.putLong(VenomSphereData.NBT_EXPIRE_TICK, expireTick);

		if (stacks > 0) {
			target.addEffect(new MobEffectInstance(
					TimothatysTrinketsModMobEffects.CORROSIVE_TOXICITY,
					VenomSphereData.EFFECT_DURATION_TICKS,
					stacks - 1,
					false,
					true,
					true
			), player);
		}

		return new StackResult(false, stackAdded, stacks);
	}

	public static void forgetTarget(Player player, LivingEntity target) {
		if (player == null || target == null)
			return;

		CompoundTag playerData = player.getPersistentData();
		ListTag targets = playerData.getList(VenomSphereData.NBT_ACTIVE_TARGETS, Tag.TAG_COMPOUND);
		UUID targetUuid = target.getUUID();
		boolean changed = false;

		for (int index = targets.size() - 1; index >= 0; index--) {
			CompoundTag entry = targets.getCompound(index);
			if (!entry.hasUUID(VenomSphereData.NBT_TARGET_UUID) || targetUuid.equals(entry.getUUID(VenomSphereData.NBT_TARGET_UUID))) {
				targets.remove(index);
				changed = true;
			}
		}

		if (changed) {
			playerData.put(VenomSphereData.NBT_ACTIVE_TARGETS, targets);
		}
	}

	public static void forgetOwnerFromTargetData(LivingEntity target) {
		if (target == null)
			return;

		CompoundTag targetData = target.getPersistentData();
		if (!targetData.hasUUID(VenomSphereData.NBT_STACK_OWNER_UUID)) {
			clearTargetData(target);
			return;
		}

		UUID ownerUuid = targetData.getUUID(VenomSphereData.NBT_STACK_OWNER_UUID);
		if (target.level() instanceof ServerLevel serverLevel) {
			ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
			if (owner != null) {
				forgetTarget(owner, target);
			}
		}

		clearTargetData(target);
	}

	public static void clearTargetData(LivingEntity target) {
		if (target == null)
			return;

		CompoundTag targetData = target.getPersistentData();
		targetData.remove(VenomSphereData.NBT_STACK_OWNER_UUID);
		targetData.remove(VenomSphereData.NBT_HITS);
		targetData.remove(VenomSphereData.NBT_STACKS);
		targetData.remove(VenomSphereData.NBT_EXPIRE_TICK);
	}

	private static boolean canUseTargetSlot(Player player, LivingEntity target, long now) {
		CompoundTag playerData = player.getPersistentData();
		ListTag targets = cleanExpiredTargets(playerData.getList(VenomSphereData.NBT_ACTIVE_TARGETS, Tag.TAG_COMPOUND), now);
		boolean alreadyTracked = containsTarget(targets, target.getUUID());
		playerData.put(VenomSphereData.NBT_ACTIVE_TARGETS, targets);
		return alreadyTracked || targets.size() < VenomSphereData.MAX_ACTIVE_TARGETS;
	}

	private static void refreshActiveTarget(Player player, LivingEntity target, long now) {
		CompoundTag playerData = player.getPersistentData();
		ListTag targets = cleanExpiredTargets(playerData.getList(VenomSphereData.NBT_ACTIVE_TARGETS, Tag.TAG_COMPOUND), now);
		UUID targetUuid = target.getUUID();
		long expireTick = now + VenomSphereData.EFFECT_DURATION_TICKS;

		for (int index = 0; index < targets.size(); index++) {
			CompoundTag entry = targets.getCompound(index);
			if (entry.hasUUID(VenomSphereData.NBT_TARGET_UUID) && targetUuid.equals(entry.getUUID(VenomSphereData.NBT_TARGET_UUID))) {
				entry.putLong(VenomSphereData.NBT_TARGET_EXPIRE_TICK, expireTick);
				playerData.put(VenomSphereData.NBT_ACTIVE_TARGETS, targets);
				return;
			}
		}

		CompoundTag entry = new CompoundTag();
		entry.putUUID(VenomSphereData.NBT_TARGET_UUID, targetUuid);
		entry.putLong(VenomSphereData.NBT_TARGET_EXPIRE_TICK, expireTick);
		targets.add(entry);
		playerData.put(VenomSphereData.NBT_ACTIVE_TARGETS, targets);
	}

	private static ListTag cleanExpiredTargets(ListTag targets, long now) {
		for (int index = targets.size() - 1; index >= 0; index--) {
			CompoundTag entry = targets.getCompound(index);
			if (!entry.hasUUID(VenomSphereData.NBT_TARGET_UUID) || now > entry.getLong(VenomSphereData.NBT_TARGET_EXPIRE_TICK)) {
				targets.remove(index);
			}
		}
		return targets;
	}

	private static boolean containsTarget(ListTag targets, UUID targetUuid) {
		for (int index = 0; index < targets.size(); index++) {
			CompoundTag entry = targets.getCompound(index);
			if (entry.hasUUID(VenomSphereData.NBT_TARGET_UUID) && targetUuid.equals(entry.getUUID(VenomSphereData.NBT_TARGET_UUID))) {
				return true;
			}
		}
		return false;
	}

	public record StackResult(boolean rejected, boolean stackAdded, int stacks) {
		public static StackResult rejectedResult() {
			return new StackResult(true, false, 0);
		}
	}
}
