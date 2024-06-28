package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.event.button.ButtonHandler;
import me.thosea.developersdungeon.util.PChannelUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class DeleteChannelCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("deletechannel", "Delete a private channel.");
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
			}

			if(!member.getId().equals(ownerId) && !Utils.isAdmin(member)) {
				hook.editOriginal("You can't do that!").queue();
				return;
			}

			hook.editOriginal("Are you SURE you want to delete this channel? All messages and attachments will be gone!")
					.setActionRow(Button.danger(ButtonHandler.ID_PCHANNEL_DELETE + "-" + ownerId, "Delete"))
					.queue();
		}));
	}
}
