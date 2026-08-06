package net.timothaty.timothatystrinkets.mechanics.cleric_quests;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.timothaty.timothatystrinkets.mechanics.cleric_quests.humility.HumilityStage;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.sacrament.SacramentStage;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.stream.Collectors;

public final class ClericQuestDebugCommands {
	private ClericQuestDebugCommands() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
		return Commands.literal("cleric")
			.then(Commands.literal("status")
				.executes(context -> status(context.getSource(), context.getSource().getPlayerOrException()))
				.then(Commands.argument("player", EntityArgument.player())
					.executes(context -> status(context.getSource(), EntityArgument.getPlayer(context, "player")))))
			.then(Commands.literal("reset")
				.then(Commands.argument("player", EntityArgument.player())
					.executes(context -> reset(context.getSource(), EntityArgument.getPlayer(context, "player")))))
			.then(Commands.literal("complete_deeds")
				.then(Commands.argument("player", EntityArgument.player())
					.executes(context -> completeDeeds(context.getSource(), EntityArgument.getPlayer(context, "player")))))
			.then(Commands.literal("complete_hunt")
				.then(Commands.argument("player", EntityArgument.player())
					.executes(context -> completeHunt(context.getSource(), EntityArgument.getPlayer(context, "player")))))
			.then(Commands.literal("set_fasting_seconds")
				.then(Commands.argument("player", EntityArgument.player())
					.then(Commands.argument("seconds", IntegerArgumentType.integer(0, 600))
						.executes(context -> setFastingSeconds(
							context.getSource(),
							EntityArgument.getPlayer(context, "player"),
							IntegerArgumentType.getInteger(context, "seconds")
						)))));
	}

	private static int status(CommandSourceStack source, ServerPlayer player) {
		ClericQuestProgress progress = ClericQuestSavedData.get(player.serverLevel()).get(player.getUUID());
		if (progress == null) {
			source.sendSuccess(() -> Component.literal(player.getGameProfile().getName() + ": no cleric quest progress"), false);
			return 1;
		}
		String targets = progress.sacramentTargets().stream().map(Object::toString).collect(Collectors.joining(", "));
		source.sendSuccess(() -> Component.literal("player=" + player.getUUID()
			+ ", humility=" + progress.humilityStage()
			+ ", humility_cleric=" + value(progress.humilityClericId())
			+ ", deeds=0x" + Integer.toHexString(progress.humilityDeedMask())
			+ ", humility_completed=" + progress.humilityCompleted()), false);
		source.sendSuccess(() -> Component.literal("sacrament=" + progress.sacramentStage()
			+ ", sacrament_cleric=" + value(progress.sacramentClericId())
			+ ", offerings=0x" + Integer.toHexString(progress.sacramentOfferingMask())
			+ ", targets=[" + targets + "]"
			+ ", kills=0x" + Integer.toHexString(progress.sacramentKilledMask())
			+ ", fasting=" + progress.fastingSeconds()
			+ ", fasting_started=" + progress.fastingHasStarted()
			+ ", grace=" + progress.desertExitGraceSeconds()
			+ ", sacrament_completed=" + progress.sacramentCompleted()), false);
		return 1;
	}

	private static int reset(CommandSourceStack source, ServerPlayer player) {
		ClericQuestSavedData.get(player.serverLevel()).remove(player.getUUID());
		source.sendSuccess(() -> Component.literal("Reset cleric quest progress for " + player.getGameProfile().getName()), true);
		return 1;
	}

	private static int completeDeeds(CommandSourceStack source, ServerPlayer player) {
		ClericQuestSavedData data = ClericQuestSavedData.get(player.serverLevel());
		ClericQuestProgress progress = data.get(player.getUUID());
		if (progress == null || progress.humilityStage() != HumilityStage.DEEDS_ACTIVE) {
			source.sendFailure(Component.literal("Player does not have active Humility deeds."));
			return 0;
		}
		progress.setHumilityDeedMask(0b111);
		progress.setHumilityStage(HumilityStage.REWARD_READY);
		data.changed(player.getUUID());
		source.sendSuccess(() -> Component.literal("Humility deeds completed for " + player.getGameProfile().getName()), true);
		return 1;
	}

	private static int completeHunt(CommandSourceStack source, ServerPlayer player) {
		ClericQuestSavedData data = ClericQuestSavedData.get(player.serverLevel());
		ClericQuestProgress progress = data.get(player.getUUID());
		if (progress == null || progress.sacramentStage() != SacramentStage.HUNT_ACTIVE || progress.sacramentTargets().size() != 3) {
			source.sendFailure(Component.literal("Player does not have a valid active Sacrament hunt."));
			return 0;
		}
		progress.setSacramentKilledMask(0b111);
		progress.setSacramentStage(SacramentStage.HUNT_RETURN);
		data.changed(player.getUUID());
		source.sendSuccess(() -> Component.literal("Sacrament hunt completed for " + player.getGameProfile().getName()), true);
		return 1;
	}

	private static int setFastingSeconds(CommandSourceStack source, ServerPlayer player, int seconds) {
		ClericQuestSavedData data = ClericQuestSavedData.get(player.serverLevel());
		ClericQuestProgress progress = data.get(player.getUUID());
		if (progress == null || progress.sacramentStage() != SacramentStage.FAST_ACTIVE) {
			source.sendFailure(Component.literal("Player does not have an active Sacrament fast."));
			return 0;
		}
		progress.setFastingSeconds(seconds);
		progress.setFastingHasStarted(seconds > 0);
		progress.setDesertExitGraceSeconds(0);
		data.changed(player.getUUID());
		source.sendSuccess(() -> Component.literal("Fasting progress set to " + seconds + " seconds for " + player.getGameProfile().getName()), true);
		return 1;
	}

	private static String value(Object value) {
		return value == null ? "none" : value.toString();
	}
}
