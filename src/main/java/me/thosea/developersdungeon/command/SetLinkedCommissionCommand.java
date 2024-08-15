package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.PChannelUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class SetLinkedCommissionCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("setlinkedcommission", "Set the commission thread linked in your private channel.")
				.addOption(OptionType.CHANNEL, "channel", "Commission thread", true);
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		var channel = event.getChannel();
		if(!PChannelUtils.isPrivateChannel(channel)) {
			event.reply("Not a private channel.").setEphemeral(true).queue();
			return;
		} else if(PChannelUtils.isArchived(channel)) {
			event.reply("This channel is archived!").setEphemeral(true).queue();
			return;
		}

		var thread = event.getOption("channel", OptionMapping::getAsChannel);
		if(!ForumUtils.isCommissionRequest(thread)) {
			event.reply("That's not a commission thread!").setEphemeral(true).queue();
			return;
		}

		event.deferReply().setEphemeral(true).queue(hook -> PChannelUtils.getMessageAndOwner(channel, (msg, ownerId) -> {
			if(ownerId == null) {
				hook.editOriginal("Could not find the bot message.").queue();
				return;
			} else if(!member.getId().equals(ownerId) && !Utils.isAdmin(member)) {
				hook.editOriginal("You can't do that!").queue();
				return;
			}

			MessageEmbed embed = PChannelUtils.makeEmbed("<@" + ownerId + ">", thread.getJumpUrl());
			msg.editMessageEmbeds(embed).queue();
			hook.editOriginal("Done > " + msg.getJumpUrl()).setEmbeds(embed).queue();
		}));
	}
}