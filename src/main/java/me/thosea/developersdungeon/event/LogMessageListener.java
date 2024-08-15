package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.util.Constants;
import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.PChannelUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class LogMessageListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if(!logChannel(event.getChannel())) return;

		Utils.logChannel("%s - %s: %s > %s",
				event.getChannel(),
				event.getMember(),
				event.getMessage().getContentRaw(),
				event.getMessage());
	}

	private boolean logChannel(MessageChannelUnion channel) {
		if(ForumUtils.isCommissionRequest(channel)) return true;
		if(PChannelUtils.isPrivateChannel(channel)) return true;
		if(channel.getIdLong() == Constants.Channels.VERIFY) return true;
		return false;
	}
}