package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.command.EchoCommand;
import me.thosea.developersdungeon.command.EchoCommand.EchoEntry;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EchoMessageListener extends ListenerAdapter {
	public static long lastEchoChannel;

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		Member member = event.getMember();
		if(member == null) return;

		EchoEntry entry = EchoCommand.ECHOS.get(member.getIdLong());
		if(entry == null || !entry.channel().equals(event.getChannel())) return;

		EchoCommand.ECHOS.remove(member.getIdLong());
		lastEchoChannel = event.getChannel().getIdLong();
		entry.messageDeleter().run();
		entry.echoHandler().accept(member, event.getMessage());
	}
}