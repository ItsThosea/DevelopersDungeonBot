package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.Constants.Channels;
import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.Duration;

public class ForumListener extends ListenerAdapter {
	@Override
	public void onChannelCreate(ChannelCreateEvent event) {
		if(!(event.getChannel() instanceof ThreadChannel channel)) return;

		long parentId = channel.getParentChannel().getIdLong();
		if(parentId != Channels.COMMISSIONS) return;

		// we have to wait for the first message before we can send our own messages
		Main.jda.listenOnce(MessageReceivedEvent.class)
				.filter(e -> e.getChannel().getIdLong() == channel.getIdLong())
				.timeout(Duration.ofSeconds(30), () -> {
					Utils.logChannel("Warning: Thread %s didn't have its first message sent", channel);
				})
				.subscribe(_ -> {
					Main.guild.retrieveMemberById(channel.getOwnerIdLong()).queue(member -> {
						Utils.logChannel("%s made commission request %s", member, channel);
						ForumUtils.sendBotMessage(channel);
					});
				});
	}
}