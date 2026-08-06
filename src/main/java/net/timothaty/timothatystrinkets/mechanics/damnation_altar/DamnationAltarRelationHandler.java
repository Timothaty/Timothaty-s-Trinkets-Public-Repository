package net.timothaty.timothatystrinkets.mechanics.damnation_altar;


import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public class DamnationAltarRelationHandler {

	private static final String REL_KEY = "tt_damnation_altar_relation";
	private static final int DEFAULT_RELATION = 100;
	private static final String ALTAR_RELATION_TRANSLATION_KEY = "message.timothatys_trinkets.altar_relation";

	public static int getOrInitRelation(ServerPlayer player) {
		if (!player.getPersistentData().contains(REL_KEY, Tag.TAG_INT)) {
			player.getPersistentData().putInt(REL_KEY, DEFAULT_RELATION);
			return DEFAULT_RELATION;
		}
		return player.getPersistentData().getInt(REL_KEY);
	}

	public static void setRelation(ServerPlayer player, int value) {
		player.getPersistentData().putInt(REL_KEY, value);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
		if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) return;
		if (!oldPlayer.getPersistentData().contains(REL_KEY, Tag.TAG_INT)) return;

		newPlayer.getPersistentData().putInt(REL_KEY, oldPlayer.getPersistentData().getInt(REL_KEY));
	}

	public static void displayRelation(ServerPlayer player) {
		int relation = getOrInitRelation(player);
		player.displayClientMessage(
				Component.translatable(ALTAR_RELATION_TRANSLATION_KEY).append(" " + relation + "/" + DEFAULT_RELATION),
				true
		);
	}
}
