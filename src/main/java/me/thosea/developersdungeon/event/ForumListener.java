package me.thosea.developersdungeon.event;

import it.unimi.dsi.fastutil.longs.LongOpenHashBigSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.Constants;
import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

public class ForumListener extends ListenerAdapter {
	private static final LongSet WAITING_FOR_MESSAGE = new LongOpenHashBigSet();

	@Override
	public void onChannelCreate(ChannelCreateEvent event) {
		if(!(event.getChannel() instanceof ThreadChannel channel)) return;

		long parentId = channel.getParentChannel().getIdLong();
		if(parentId != Constants.Channels.COMMISSIONS) return;

		WAITING_FOR_MESSAGE.add(channel.getIdLong());
		Utils.doLater(TimeUnit.SECONDS, 30, () -> {
			if(WAITING_FOR_MESSAGE.contains(channel.getIdLong())) {
				Utils.logChannel("Warning: Thread %s didn't have its first message sent", channel);
			}
		});
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if(!WAITING_FOR_MESSAGE.remove(event.getChannel().getIdLong())) return;

		ThreadChannel channel = event.getChannel().asThreadChannel();

		Main.guild.retrieveMemberById(channel.getOwnerIdLong()).queue(member -> {
			Utils.logChannel("%s made commission request %s", member, channel);
			ForumUtils.sendBotMessage(channel);
		});
	}

}