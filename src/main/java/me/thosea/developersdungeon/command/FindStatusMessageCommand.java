package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.PChannelUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FindStatusMessageCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("findstatusmessage", "Find and display the bot status message of a commission thread or private channel.");
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		MessageChannel channel = event.getChannel();
		BiConsumer<MessageChannel, Consumer<Message>> messageGetter;

		if(ForumUtils.isCommissionRequest(channel)) {
			messageGetter = ForumUtils::getBotMessage;
		} else if(PChannelUtils.isPrivateChannel(channel)) {
			messageGetter = PChannelUtils::getBotMessage;
		} else {
			event.reply("Not a commission request thread or private channel.")
					.setEphemeral(true)
					.queue();
			return;
		}

		event.deferReply().setEphemeral(true).queue(hook -> messageGetter.accept(channel, message -> {
			if(message == null) {
				hook.editOriginal("Could not find hte bot message.").queue();
				return;
			}

			hook.editOriginal("Found it > " + message.getJumpUrl())
					.setEmbeds(message.getEmbeds())
					.setAllowedMentions(List.of())
					.setActionRow(message.getComponents().isEmpty()
							? List.of()
							: message.getActionRows().get(0).getComponents())
					.queue();
		}));
	}
}
