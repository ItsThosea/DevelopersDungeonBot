package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.button.ButtonHandler;
import me.thosea.developersdungeon.util.PChannelUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class LeaveChannelCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("leavechannel", "Leave a private channel.");
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		if(!PChannelUtils.isPrivateChannel(event.getChannel())) {
			event.reply("Not a private channel.").setEphemeral(true).queue();
			return;
		}

		var channel = event.getChannel();
		event.deferReply().setEphemeral(true).queue(hook -> PChannelUtils.getOwner(channel, ownerId -> {
			if(ownerId == null) {
				hook.editOriginal("Could not find the bot message.").queue();
				return;
			} else if(member.getId().equals(ownerId)) {
				hook.editOriginal("You can't do that because you own the channel. Delete or archive it instead.").queue();
				return;
			}

			hook.editOriginal("Are you SURE you want to leave the channel?\nYou'll lose access to it permanently unless re-added!")
					.setActionRow(Button.danger(ButtonHandler.ID_PCHANNEL_LEAVE, "Leave"))
					.queue();
		}));
	}
}