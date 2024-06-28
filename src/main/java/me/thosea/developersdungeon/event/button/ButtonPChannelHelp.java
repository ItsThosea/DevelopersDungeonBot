package me.thosea.developersdungeon.event.button;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.awt.Color;

public class ButtonPChannelHelp implements ButtonHandler {
	@Override
	public String getId() {
		return ButtonHandler.ID_PCHANNEL_HELP;
	}

	@Override
	public void handle(Member member, ButtonInteractionEvent event, String[] args) {
		event.reply("Commands:")
				.setEmbeds(new EmbedBuilder().setDescription("""
									/commissionchannels - Manipulate channels displayed in your commission forum post.
									/deletechannel - Delete this channel.
									/renamechannel - Rename this channel.
									To add or remove people or teams from accessing this channel, use the discord channel settings menu.
									""")
						.setColor(new Color(50, 168, 82))
						.build())
				.setEphemeral(true)
				.queue();
	}
}
