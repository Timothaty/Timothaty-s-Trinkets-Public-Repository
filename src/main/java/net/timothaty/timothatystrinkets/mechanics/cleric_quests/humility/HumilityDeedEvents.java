package net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class HumilityDeedEvents {
	private HumilityDeedEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onGolemInteract(PlayerInteractEvent.EntityInteract event) {
		if (event.isCanceled()
				|| event.getHand() != InteractionHand.MAIN_HAND
				|| !(event.getEntity() instanceof ServerPlayer player)
				|| !(event.getTarget() instanceof IronGolem golem)
				|| !(player.level() instanceof ServerLevel level)
				|| !event.getItemStack().is(Items.IRON_INGOT)
				|| golem.isPlayerCreated()
				|| golem.getHealth() >= golem.getMaxHealth())
			return;
		HumilityGolemRepairTracker.track(level, golem, player.getUUID());
	}

	@SubscribeEvent
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| !(event.getLevel() instanceof ServerLevel level)
				|| (!event.getPlacedBlock().is(Blocks.CARVED_PUMPKIN) && !event.getPlacedBlock().is(Blocks.JACK_O_LANTERN)))
			return;
		HumilityGolemCreationTracker.track(level, event.getPos(), player.getUUID(), HumilityGolemCreationTracker.ActionType.PLACEMENT);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onPumpkinCarved(PlayerInteractEvent.RightClickBlock event) {
		if (event.isCanceled()
				|| event.getHand() != InteractionHand.MAIN_HAND
				|| !(event.getEntity() instanceof ServerPlayer player)
				|| !(event.getLevel() instanceof ServerLevel level)
				|| !event.getItemStack().is(Items.SHEARS)
				|| !level.getBlockState(event.getPos()).is(Blocks.PUMPKIN))
			return;
		HumilityGolemCreationTracker.track(level, event.getPos(), player.getUUID(), HumilityGolemCreationTracker.ActionType.CARVING);
	}

	@SubscribeEvent
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if (event.getEntity() instanceof IronGolem golem && event.getLevel() instanceof ServerLevel level)
			HumilityGolemCreationTracker.match(level, golem);
	}

	@SubscribeEvent
	public static void onTrade(TradeWithVillagerEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && event.getAbstractVillager() instanceof Villager)
			HumilityQuestService.recordDeed(player.getServer(), player.getUUID(), HumilityDeedType.TRADE_WITH_VILLAGER);
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onLivingDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof Raider raider)
				|| (raider.getType() != net.minecraft.world.entity.EntityType.EVOKER && raider.getType() != net.minecraft.world.entity.EntityType.RAVAGER)
				|| raider.getCurrentRaid() != null)
			return;
		ServerPlayer killer = resolvePlayerKiller(event.getSource(), raider);
		if (killer != null)
			HumilityQuestService.recordDeed(killer.getServer(), killer.getUUID(), HumilityDeedType.SLAY_RAIDER_OUTSIDE_RAID);
	}

	public static ServerPlayer resolvePlayerKiller(DamageSource source, LivingEntity victim) {
		if (source != null && source.getEntity() instanceof ServerPlayer player)
			return player;
		Entity direct = source == null ? null : source.getDirectEntity();
		if (direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player)
			return player;
		return victim.getKillCredit() instanceof ServerPlayer player ? player : null;
	}
}
