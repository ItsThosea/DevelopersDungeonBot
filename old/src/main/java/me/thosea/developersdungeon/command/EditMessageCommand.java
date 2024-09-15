package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.other.ChannelThreadCounter;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class EditMessageCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("editbotmessage", "Edit a message sent by me.")
				.addOption(OptionType.STRING, "message", "Message ID", true)
				.addOption(OptionType.STRING, "content", "New contents", true)
				.addOption(OptionType.CHANNEL, "channel", "Override target channel");
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		if(!Utils.isAdmin(member)) {
			event.reply("You can't do that!").setEphemeral(true).queue();
			return;
		}

		Channel rawChannel = event.getOption("channel", event::getChannel, OptionMapping::getAsChannel);
		if(!(rawChannel instanceof TextChannel channel)) {
			event.reply("That's not a text channel!").setEphemeral(true).queue();
			return;
		}

		String idStr = event.getOption("message", "", OptionMapping::getAsString);
		long id;
		try {
			id = Long.parseLong(idStr);
		} catch(NumberFormatException ignored) {
			event.reply("Invalid number.").setEphemeral(true).queue();
			return;
		}

		String content = event.getOption("content", "", OptionMapping::getAsString);

		event.deferReply().setEphemeral(true).queue(hook -> channel.retrieveMessageById(id).queue(msg -> {
			if(!Main.jda.getSelfUser().equals(msg.getAuthor())) {
				hook.editOriginal("I didn't say that!").queue();
				return;
			}

			msg.editMessage(content)
					.and(hook.editOriginal("Edited > " + msg.getJumpUrl()))
					.queue();

			ChannelThreadCounter counter = ChannelThreadCounter.getCounterByCountMessage(msg.getIdLong());
			if(counter != null) {
				try {
					counter.setCount(Integer.parseInt(content));
				} catch(NumberFormatException ignored) {} // womp womp
			}
		}, _ -> {
			hook.editOriginal("No message found!").queue();
		}));
	}
}