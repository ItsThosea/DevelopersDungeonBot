package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;
import java.util.function.Consumer;

public class SetCommissionStatusCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash(
						"setcommissionstatus",
						"Run in the forum channel of your commission to change its visible status.")
				.addOption(OptionType.STRING, "status", "Status", true);
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		event.deferReply().setEphemeral(true).queue(hook -> {
			if(!ForumUtils.isCommissionRequest(event.getChannel())) {
				hook.editOriginal("Run this in the forum channel of your commission.").queue();
				return;
			}

			String status = event.getOption("status", OptionMapping::getAsString);
			assert status != null;
			handle(member, event.getChannel().asThreadChannel(), status, msg -> {
				hook.editOriginal(msg).queue();
			});
		});
	}

	public static void handle(Member member, ThreadChannel channel, String status, Consumer<String> sender) {
		if(member.getIdLong() != channel.getOwnerIdLong() && !Utils.isAdmin(member)) {
			sender.accept("Only the commission owner and admins can edit this.");
			return;
		}

		ForumUtils.getBotMessage(channel, message -> {
			if(message == null) {
				sender.accept("Could not find the bot message.");
				return;
			}

			message.editMessageEmbeds(
							ForumUtils.makeStatusEmbed(status),
							message.getEmbeds().get(1))
					.setAllowedMentions(List.of())
					.queue();

			channel.sendMessage(member.getAsMention() + " changed the commission status to " + status)
					.setAllowedMentions(List.of())
					.queue();

			sender.accept("Edited the status to " + status + " > " + message.getJumpUrl());
			Utils.logMinor("%s edited commission status of channel %s to %s", member, channel, status);
		});
	}
}