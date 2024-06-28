package me.thosea.developersdungeon.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface CommandHandler {
	Map<String, CommandHandler> COMMANDS = new HashMap<>();

	static List<SlashCommandData> buildCommands() {
		List<CommandHandler> handlers = List.of(
				new EchoCommand(),
				new SetCommissionStateCommand(),
				new DebugCommand(),
				new CommissionChannelsCommand(),
				new FindStateMessageCommand(),
				new MakePrivateChannelCommand(),
				new DeleteChannelCommand(),
				new RenameChannelCommand(),
				new TeamCommand()
		);
		List<SlashCommandData> result = new ArrayList<>(handlers.size());

		for(CommandHandler handler : handlers) {
			SlashCommandData data = handler.makeCommandData();
			result.add(data);
			COMMANDS.put(data.getName(), handler);
		}

		return result;
	}

	SlashCommandData makeCommandData();
	void handle(Member member, SlashCommandInteraction event);
}
