package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.other.ChannelThreadCounter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AutoThreadListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		ChannelThreadCounter counter = ChannelThreadCounter.getCounter(event.getChannel().getIdLong());
		if(counter == null) return;
		if(Main.jda.getSelfUser().equals(event.getAuthor())) return;

		Message msg = event.getMessage();
		if(msg.getType() != MessageType.DEFAULT) return;

		counter.makeThread(msg);
	}

	@Override
	public void onMessageDelete(MessageDeleteEvent event) {
		ChannelThreadCounter counter = ChannelThreadCounter.getCounter(event.getChannel().getIdLong());
		if(counter != null) {
			counter.removeMessage(event.getMessageIdLong());
		}
	}
}