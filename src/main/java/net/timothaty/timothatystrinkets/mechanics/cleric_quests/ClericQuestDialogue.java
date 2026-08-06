package net.timothaty.timothatystrinkets.mechanics.cleric_quests;

import net.timothaty.timothatystrinkets.mechanics.dialogue.DialogueHudSender;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.Villager;

import java.util.List;

public final class ClericQuestDialogue {
	private static final String SPEAKER_KEY = "dialogue.timothatys_trinkets.cleric.speaker";
	private static final int LETTER_DELAY_TICKS = 1;
	private static final int HOLD_TICKS = 80;

	private ClericQuestDialogue() {
	}

	public static void show(Villager cleric, ServerPlayer player, String lineKey) {
		if (!(cleric.level() instanceof ServerLevel level) || player.serverLevel() != level)
			return;
		DialogueHudSender.sendToPlayer(player, SPEAKER_KEY, List.of(lineKey), LETTER_DELAY_TICKS, HOLD_TICKS);
		player.connection.send(new ClientboundSoundPacket(
			BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.VILLAGER_YES),
			SoundSource.VOICE,
			cleric.getX(),
			cleric.getY(),
			cleric.getZ(),
			1.0F,
			1.0F,
			level.getRandom().nextLong()
		));
	}
}
