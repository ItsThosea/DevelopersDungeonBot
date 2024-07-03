package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.other.PingResponse;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Locale;

public class PingResponseMessageListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		Message message = event.getMessage();
		if(!Utils.isBeingPinged(message)) return;

		String content = message.getContentRaw().toLowerCase(Locale.ENGLISH);

		for(PingResponse response : PingResponse.PIPELINE) {
			if(response.matches(content) == response.mustMatch()) {
				response.modifyMessage(message.reply(response.getResponse(content))).queue();
				break;
			}
		}
	}
}
