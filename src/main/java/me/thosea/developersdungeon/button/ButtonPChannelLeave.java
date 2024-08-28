package me.thosea.developersdungeon.button;

import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.List;

public class ButtonPChannelLeave implements ButtonHandler {
	@Override
	public String getId() {
		return ButtonHandler.ID_PCHANNEL_LEAVE;
	}

	@Override
	public void handle(Member member, ButtonInteractionEvent event, String[] args) {
		GuildMessageChannel channel = event.getGuildChannel();
		channel.getPermissionContainer()
				.upsertPermissionOverride(member)
				.setDenied(Permission.VIEW_CHANNEL)
				.reason("User left channel")
				.queue();
		channel.sendMessage(member.getAsMention() + " left the channel.")
				.setAllowedMentions(List.of())
				.queue();

		Utils.logMinor("%s left private channel %s", member, channel);
		event.reply("You have left the private channel.")
				.setEphemeral(true)
				.and(event.getMessage().delete())
				.queue();
	}
}