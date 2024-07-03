package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.Constants.Categories;
import me.thosea.developersdungeon.util.PChannelUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public class ArchiveCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("archivechannel", "Archive your private channel.");
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		var channel = event.getChannel();
		if(!PChannelUtils.isPrivateChannel(channel)) {
			event.reply("Not a private channel.").setEphemeral(true).queue();
			return;
		} else if(PChannelUtils.isArchived(channel)) {
			event.reply("This channel is already archived!").setEphemeral(true).queue();
			return;
		}

		event.deferReply().setEphemeral(true).queue(hook -> PChannelUtils.getOwner(channel, ownerId -> {
			if(ownerId == null) {
				hook.editOriginal("Could not find the bot message.").queue();
				return;
			} else if(!member.getId().equals(ownerId) && !Utils.isAdmin(member)) {
				hook.editOriginal("You can't do that!").queue();
				return;
			}

			((ICategorizableChannel) channel).getManager()
					.setParent(Main.guild.getCategoryById(Categories.ARCHIVED_CHANNELS_CATEGORY))
					.queue();

			hook.editOriginal("Archived the channel.").queue();
			Utils.logChannel("%s archived channel %s", member, channel);
			channel.sendMessage("This channel has been archived by " + member.getAsMention() + ".")
					.setAllowedMentions(List.of())
					.queue();
		}));
	}
}
