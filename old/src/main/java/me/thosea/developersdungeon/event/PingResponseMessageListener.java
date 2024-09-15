package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.other.ChannelThreadCounter;
import me.thosea.developersdungeon.other.PingResponse;
import me.thosea.developersdungeon.util.Constants.Channels;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;
import java.util.Locale;

public class PingResponseMessageListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		Message message = event.getMessage();
		if(Channels.BOTS > 0 && event.getChannel().getIdLong() != Channels.BOTS) return;
		if(!Utils.isBeingPinged(message)) return;
		if(ChannelThreadCounter.getCounter(event.getChannel().getIdLong()) != null) return;

		String content = message.getContentRaw().toLowerCase(Locale.ENGLISH);

		for(PingResponse response : PingResponse.PIPELINE) {
			if(response.matches(content) == response.mustMatch()) {
				if(response.mustMatch()
						&& (content.contains("say") || content.contains("send"))
						&& content.contains("if")) {
					message.reply(content.contains("say") ? "Say whaaat?" : "Send whaaat?").queue();
				} else {
					message.reply(response.getResponse(content)
									.replace("$cdn/", "https://devsdungeon-cdn.pages.dev/pings/"))
							.setAllowedMentions(List.of()).queue();
				}
				break;
			}
		}
	}
}