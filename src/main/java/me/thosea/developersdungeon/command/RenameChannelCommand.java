package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.util.PChannelUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public class RenameChannelCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("renamechannel", "Rename your private channel.")
				.addOption(OptionType.STRING, "name", "Name", true);
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		if(!PChannelUtils.isPrivateChannel(event.getChannel())) {
			event.reply("Not a private channel.").setEphemeral(true).queue();
			return;
		}

		var channel = event.getChannel();
		String oldName = channel.getName();
		String prefix = oldName.substring(0, oldName.indexOf('-'));
		String newName = prefix + "-" + event.getOption("name", OptionMapping::getAsString);

		if(newName.length() > 100) {
			event.reply("That channel name is too long.").setEphemeral(true).queue();
			return;
		}

		event.deferReply().setEphemeral(true).queue(hook -> PChannelUtils.getOwner(channel, ownerId -> {
			if(ownerId == null) {
				hook.editOriginal("Could not find the bot message.").queue();
				return;
			}

			if(!member.getId().equals(ownerId) && !Utils.isAdmin(member)) {
				hook.editOriginal("You can't do that!").queue();
				return;
			}

			if(channel instanceof TextChannel text) {
				text.getManager().setName(newName).queue();
			} else if(channel instanceof VoiceChannel voice) {
				voice.getManager().setName(newName).queue();
			} else {
				hook.editOriginal("Not a text or voice channel? This is a bug.").queue();
				return;
			}

			Utils.logChannel("%s renamed channel #%s to #%s > %s", member, oldName, newName, channel);

			hook.editOriginal("Renamed channel.").queue();
			channel.sendMessage(String.format(
							"%s renamed this channel from #%s to #%s.",
							member.getAsMention(),
							oldName, newName))
					.setAllowedMentions(List.of())
					.queue();
		}));
	}
}
