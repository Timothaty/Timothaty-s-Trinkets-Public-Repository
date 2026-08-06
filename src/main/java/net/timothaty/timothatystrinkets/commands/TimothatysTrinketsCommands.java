package net.timothaty.timothatystrinkets.commands;

import com.mojang.brigadier.CommandDispatcher;

import net.timothaty.timothatystrinkets.TimothatysTrinketsMod;
import net.timothaty.timothatystrinkets.mechanics.cleric_quests.ClericQuestDebugCommands;
import net.timothaty.timothatystrinkets.mechanics.striker_acquisition.StrikerDebugCommands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = TimothatysTrinketsMod.MODID)
public final class TimothatysTrinketsCommands {
	private TimothatysTrinketsCommands() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		register(event.getDispatcher());
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("ttr")
				.then(Commands.literal("debug")
					.requires(source -> source.hasPermission(2))
					.then(ClericQuestDebugCommands.createCommand())
					.then(StrikerDebugCommands.createCommand()))
		);
	}
}
