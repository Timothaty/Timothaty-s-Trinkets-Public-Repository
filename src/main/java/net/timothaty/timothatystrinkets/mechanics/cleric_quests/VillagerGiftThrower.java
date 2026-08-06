package net.timothaty.timothatystrinkets.mechanics.cleric_quests;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VillagerGiftThrower {
	private static final int DROP_INTERVAL_TICKS = 5;
	private static final Map<UUID, GiftSequence> SEQUENCES = new HashMap<>();

	private VillagerGiftThrower() {
	}

	public static boolean throwStack(ServerLevel level, Villager villager, ServerPlayer recipient, ItemStack stack, SoundEvent sound) {
		if (stack.isEmpty())
			return false;
		return throwStacks(level, villager, recipient, List.of(stack), sound);
	}

	public static boolean throwStacks(ServerLevel level, Villager villager, ServerPlayer recipient, List<ItemStack> stacks, SoundEvent sound) {
		if (stacks.isEmpty() || stacks.stream().anyMatch(ItemStack::isEmpty))
			return false;
		Vec3 direction = recipient.position().subtract(villager.position()).multiply(1.0D, 0.0D, 1.0D);
		if (direction.lengthSqr() > 1.0E-6D)
			direction = direction.normalize();
		else {
			direction = villager.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
			direction = direction.lengthSqr() > 1.0E-6D ? direction.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
		}
		List<ItemEntity> spawned = new ArrayList<>(stacks.size());
		for (ItemStack stack : stacks) {
			ItemEntity item = new ItemEntity(level, villager.getX(), villager.getY() + villager.getBbHeight() * 0.65D, villager.getZ(), stack.copy());
			item.setPickUpDelay(15);
			item.setDeltaMovement(direction.x * 0.22D, 0.20D, direction.z * 0.22D);
			if (!level.addFreshEntity(item)) {
				spawned.forEach(Entity::discard);
				return false;
			}
			spawned.add(item);
		}
		level.playSound(null, villager.blockPosition(), sound, SoundSource.NEUTRAL, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.5F);
		return true;
	}

	public static boolean beginBonusSequence(ServerLevel level, Villager villager, ServerPlayer recipient, List<ItemStack> stacks) {
		if (stacks.isEmpty() || stacks.stream().anyMatch(ItemStack::isEmpty) || SEQUENCES.containsKey(villager.getUUID()))
			return false;
		SEQUENCES.put(villager.getUUID(), new GiftSequence(level.dimension(), villager.getUUID(), recipient.getUUID(), stacks.stream().map(ItemStack::copy).toList(), level.getGameTime() + DROP_INTERVAL_TICKS));
		return true;
	}

	public static void tick(MinecraftServer server) {
		if (SEQUENCES.isEmpty())
			return;
		Iterator<GiftSequence> iterator = SEQUENCES.values().iterator();
		while (iterator.hasNext()) {
			GiftSequence sequence = iterator.next();
			ServerLevel level = server.getLevel(sequence.dimension);
			if (level == null || level.getGameTime() < sequence.nextDropAt)
				continue;
			Entity entity = level.getEntity(sequence.clericId);
			ServerPlayer recipient = server.getPlayerList().getPlayer(sequence.recipientId);
			if (!(entity instanceof Villager villager) || recipient == null || !villager.isAlive() || !recipient.isAlive() || recipient.serverLevel() != level) {
				iterator.remove();
				continue;
			}
			if (!throwStack(level, villager, recipient, sequence.stacks.get(sequence.index), SoundEvents.DYE_USE)) {
				iterator.remove();
				continue;
			}
			sequence.index++;
			if (sequence.index >= sequence.stacks.size())
				iterator.remove();
			else
				sequence.nextDropAt = level.getGameTime() + DROP_INTERVAL_TICKS;
		}
	}

	public static boolean hasSequence(UUID clericId) {
		return SEQUENCES.containsKey(clericId);
	}

	public static boolean hasSequences() {
		return !SEQUENCES.isEmpty();
	}

	public static void cancelForCleric(UUID clericId) {
		SEQUENCES.remove(clericId);
	}

	public static void clear() {
		SEQUENCES.clear();
	}

	private static final class GiftSequence {
		private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
		private final UUID clericId;
		private final UUID recipientId;
		private final List<ItemStack> stacks;
		private int index;
		private long nextDropAt;

		private GiftSequence(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, UUID clericId, UUID recipientId, List<ItemStack> stacks, long nextDropAt) {
			this.dimension = dimension;
			this.clericId = clericId;
			this.recipientId = recipientId;
			this.stacks = stacks;
			this.nextDropAt = nextDropAt;
		}
	}
}
