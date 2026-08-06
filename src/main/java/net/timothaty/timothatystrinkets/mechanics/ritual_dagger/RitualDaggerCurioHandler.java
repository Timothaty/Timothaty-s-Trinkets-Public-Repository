package net.timothaty.timothatystrinkets.mechanics.ritual_dagger;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModParticleTypes;
import net.timothaty.timothatystrinkets.init.TimothatysTrinketsModSounds;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.core.particles.DustParticleOptions;

import org.joml.Vector3f;

import java.util.List;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public class RitualDaggerCurioHandler {

	private static final ResourceLocation RITUAL_DAGGER_ID =
			ResourceLocation.parse(TimothatysTrinketsMod.MODID + ":ritual_dagger");
	private static final ResourceLocation RITUAL_DAGGER_DEATH_LOOT_TABLE =
			ResourceLocation.parse(TimothatysTrinketsMod.MODID + ":gameplay/ritual_dagger_death");
	private static final ResourceKey<LootTable> RITUAL_DAGGER_DEATH_LOOT_TABLE_KEY =
			ResourceKey.create(Registries.LOOT_TABLE, RITUAL_DAGGER_DEATH_LOOT_TABLE);

	private static final EntityCapability<IItemHandler, Void> CURIOS_INVENTORY =
			EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath("curios", "item_handler"), IItemHandler.class);

	private static final String RITUAL_DAGGER_RESPAWN_REWARD = "ritual_dagger_respawn_reward";
	private static final String RITUAL_DAGGER_PROC_ACTIVE = "ritual_dagger_proc_active";

	private static final double REWARD_CHANCE = 0.25D;
	private static final double DURABILITY_COST_FRACTION = 0.25D;

	private static final Vector3f RITUAL_DAGGER_DEATH_DUST_COLOR = new Vector3f(0.06666667F, 0.81960785F, 0.06666667F);
	private static final float RITUAL_DAGGER_DEATH_DUST_SCALE = 1.0F;

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (!(player.level() instanceof ServerLevel serverLevel))
			return;
		if (serverLevel.getServer().isHardcore())
			return;

		clearRitualDaggerRespawnData(player);

		if (!damageRitualDagger(player))
			return;
		if (!didRitualDaggerProc(player))
			return;

		player.getPersistentData().putBoolean(RITUAL_DAGGER_PROC_ACTIVE, true);
		player.getPersistentData().putBoolean(RITUAL_DAGGER_RESPAWN_REWARD, true);

		playDeadLuckDeathSound(serverLevel, player);
		spawnDeadLuckParticles(serverLevel, player);
		spawnDeathDustParticles(serverLevel, player);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (!event.isWasDeath())
			return;

		Player original = event.getOriginal();
		Player cloned = event.getEntity();
		if (!(cloned.level() instanceof ServerLevel serverLevel))
			return;
		if (serverLevel.getServer().isHardcore())
			return;

		boolean shouldGiveReward = original.getPersistentData().getBoolean(RITUAL_DAGGER_RESPAWN_REWARD)
				|| cloned.getPersistentData().getBoolean(RITUAL_DAGGER_RESPAWN_REWARD);

		clearRitualDaggerRespawnData(original);
		clearRitualDaggerRespawnData(cloned);

		if (!shouldGiveReward)
			return;

		giveRandomRitualDaggerDeathReward(serverLevel, cloned);
	}

	private static void playDeadLuckDeathSound(ServerLevel level, Player player) {
		player.setSilent(true);
		level.playSound(
				null,
				player.getX(), player.getY(), player.getZ(),
				TimothatysTrinketsModSounds.DEAD_LUCK_DEATH.get(),
				player.getSoundSource(),
				1.0F,
				1.0F
		);
	}

	private static void spawnDeadLuckParticles(ServerLevel level, Player player) {
		double x = player.getX();
		double y = player.getY() + player.getBbHeight() * 0.5D;
		double z = player.getZ();

		level.sendParticles(
				TimothatysTrinketsModParticleTypes.DEAD_LUCK.get(),
				x, y, z,
				80,
				0.5D, 0.55D, 0.5D,
				0.01D
		);
	}

	private static void spawnDeathDustParticles(ServerLevel level, Player player) {
		double x = player.getX();
		double y = player.getY() + player.getBbHeight() * 0.5D;
		double z = player.getZ();

		level.sendParticles(
				new DustParticleOptions(RITUAL_DAGGER_DEATH_DUST_COLOR, RITUAL_DAGGER_DEATH_DUST_SCALE),
				x, y, z,
				40,
				0.5D, 0.55D, 0.5D,
				0.01D
		);
	}

	private static void giveRandomRitualDaggerDeathReward(ServerLevel level, Player player) {
		LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(RITUAL_DAGGER_DEATH_LOOT_TABLE_KEY);
		if (lootTable == LootTable.EMPTY)
			return;

		LootParams lootParams = new LootParams.Builder(level)
				.withParameter(LootContextParams.THIS_ENTITY, player)
				.withParameter(LootContextParams.ORIGIN, player.position())
				.create(LootContextParamSets.GIFT);

		List<ItemStack> generatedLoot = lootTable.getRandomItems(lootParams);
		if (generatedLoot.isEmpty())
			return;

		ItemStack reward = generatedLoot.get(level.random.nextInt(generatedLoot.size())).copy();
		player.getInventory().placeItemBackInInventory(reward);
	}

	private static boolean didRitualDaggerProc(Player player) {
		return player.getRandom().nextDouble() < REWARD_CHANCE;
	}

	private static void clearRitualDaggerRespawnData(Player player) {
		if (player == null)
			return;

		player.getPersistentData().remove(RITUAL_DAGGER_RESPAWN_REWARD);
		player.getPersistentData().remove(RITUAL_DAGGER_PROC_ACTIVE);
	}

	public static void clearRitualDaggerProcActive(Player player) {
		if (player == null)
			return;

		player.getPersistentData().remove(RITUAL_DAGGER_PROC_ACTIVE);
	}

	private static boolean damageRitualDagger(Player player) {
		IItemHandler curios = player.getCapability(CURIOS_INVENTORY, null);
		if (curios == null)
			return false;

		for (int slot = 0; slot < curios.getSlots(); slot++) {
			ItemStack stack = curios.getStackInSlot(slot);
			if (!isRitualDaggerStack(stack))
				continue;
			if (!stack.isDamageableItem())
				return true;

			int durabilityCost = Math.max(1, (int) Math.ceil(stack.getMaxDamage() * DURABILITY_COST_FRACTION));
			stack.setDamageValue(Math.min(stack.getDamageValue() + durabilityCost, stack.getMaxDamage()));

			if (stack.getDamageValue() >= stack.getMaxDamage()) {
				stack.shrink(1);
			}

			if (curios instanceof IItemHandlerModifiable modifiable) {
				modifiable.setStackInSlot(slot, stack);
			}
			return true;
		}
		return false;
	}

	public static boolean isRitualDaggerProcActive(Player player) {
		return player.getPersistentData().getBoolean(RITUAL_DAGGER_PROC_ACTIVE);
	}

	private static boolean isRitualDaggerStack(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return false;
		ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return RITUAL_DAGGER_ID.equals(key);
	}
}
