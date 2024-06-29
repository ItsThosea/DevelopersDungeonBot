package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.util.ForumUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public class FindStatusMessageCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("findstatusmessage", "Find and display the bot status message of a commission request.");
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		MessageChannel channel = event.getChannel();
		if(!ForumUtils.isCommissionRequest(channel)) {
			event.reply("Not a commission request thread.")
					.setEphemeral(true)
					.queue();
			return;
		}

		event.deferReply().setEphemeral(true).queue(hook -> ForumUtils.getBotMessage(channel, message -> {
			if(message == null) {
				hook.editOriginal("Could not find hte bot message.").queue();
				return;
			}

			hook.editOriginal("Found it > " + message.getJumpUrl())
					.setEmbeds(message.getEmbeds())
					.setAllowedMentions(List.of())
					.queue();
		}));
	}
}
