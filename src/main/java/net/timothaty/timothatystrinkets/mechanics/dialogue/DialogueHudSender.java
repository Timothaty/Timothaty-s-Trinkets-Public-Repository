package net.timothaty.timothatystrinkets.mechanics.dialogue;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.entity.DebtlordEntity;
import net.timothaty.timothatystrinkets.network.DialogueHudMessage;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DialogueHudSender {
	private static final double DEFAULT_CUBE_RANGE = 12.0D;
	private static final ResourceLocation DEBTLORD_TALK_SOUND = ResourceLocation.fromNamespaceAndPath(TimothatysTrinketsMod.MODID, "dlrd_talk");
	private static final String DEBTLORD_SUMMON_DIALOGUE_PREFIX = "dialogue.timothatys_trinkets.debtlord.summon.";
	private static final String DEBTLORD_SECOND_PHASE_DIALOGUE_PREFIX = "dialogue.timothatys_trinkets.debtlord.second_phase.";
	private static final float DEBTLORD_TALK_MIN_PITCH = 0.2F;
	private static final float DEBTLORD_TALK_MAX_PITCH = 0.4F;
	private static final int DEBTLORD_DIALOGUE_HOLD_TICKS = 70;

	private DialogueHudSender() {
	}

	public static void sendToNearbyAndForced(ServerLevel level, Vec3 source, ServerPlayer forcedRecipient, String speakerKey, List<String> lineKeys, ResourceLocation soundId, int letterDelayTicks, int lineHoldTicks, float minPitch, float maxPitch) {
		if (lineKeys.isEmpty())
			return;

		DialogueHudMessage message = new DialogueHudMessage(speakerKey, lineKeys, soundId.toString(), Math.max(1, letterDelayTicks), Math.max(0, lineHoldTicks), minPitch, maxPitch, true);
		Set<ServerPlayer> recipients = new HashSet<>();
		for (ServerPlayer player : level.players()) {
			if (isInsideCube(player.position(), source, DEFAULT_CUBE_RANGE))
				recipients.add(player);
		}
		if (forcedRecipient != null)
			recipients.add(forcedRecipient);

		for (ServerPlayer player : recipients)
			PacketDistributor.sendToPlayer(player, message);
	}

	public static void sendToPlayer(ServerPlayer player, String speakerKey, List<String> lineKeys, int letterDelayTicks, int lineHoldTicks) {
		if (player == null || lineKeys.isEmpty())
			return;
		PacketDistributor.sendToPlayer(player, new DialogueHudMessage(
			speakerKey,
			lineKeys,
			"",
			Math.max(1, letterDelayTicks),
			Math.max(0, lineHoldTicks),
			1.0F,
			1.0F,
			false
		));
	}

	public static void playDebtlordSummonLine(DebtlordEntity debtlord, ServerPlayer summoner) {
		if (!(debtlord.level() instanceof ServerLevel serverLevel))
			return;

		int variant = 1 + debtlord.getRandom().nextInt(5);
		sendToNearbyAndForced(
				serverLevel,
				debtlord.position(),
				summoner,
				debtlord.getType().getDescriptionId(),
				List.of(DEBTLORD_SUMMON_DIALOGUE_PREFIX + variant),
				DEBTLORD_TALK_SOUND,
				1,
				DEBTLORD_DIALOGUE_HOLD_TICKS,
				DEBTLORD_TALK_MIN_PITCH,
				DEBTLORD_TALK_MAX_PITCH
		);
	}

	public static void playDebtlordSecondPhaseLine(DebtlordEntity debtlord, ServerPlayer summoner) {
		if (!(debtlord.level() instanceof ServerLevel serverLevel))
			return;

		int variant = 1 + debtlord.getRandom().nextInt(7);
		sendToNearbyAndForced(
				serverLevel,
				debtlord.position(),
				summoner,
				debtlord.getType().getDescriptionId(),
				List.of(DEBTLORD_SECOND_PHASE_DIALOGUE_PREFIX + variant),
				DEBTLORD_TALK_SOUND,
				1,
				DEBTLORD_DIALOGUE_HOLD_TICKS,
				DEBTLORD_TALK_MIN_PITCH,
				DEBTLORD_TALK_MAX_PITCH
		);
	}

	private static boolean isInsideCube(Vec3 position, Vec3 center, double range) {
		return Math.abs(position.x - center.x) <= range
				&& Math.abs(position.y - center.y) <= range
				&& Math.abs(position.z - center.z) <= range;
	}
}
