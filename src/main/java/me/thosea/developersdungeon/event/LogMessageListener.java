package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.PChannelUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Locale;

public class LogMessageListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		var message = event.getMessage();
		var channel = event.getChannel();

		if(Main.jda.getSelfUser().equals(message.getAuthor()))
			return;

		if(Utils.isBeingPinged(message)) {
			String content = message.getContentRaw().toLowerCase(Locale.ENGLISH);
			message.reply(getPingResponse(content)).queue();
		}

		if(!ForumUtils.isCommissionRequest(channel) && !PChannelUtils.isPrivateChannel(channel))
			return;

		Utils.logChannel("%s: %s > %s (%s)",
				event.getMember(),
				event.getMessage().getContentRaw(),
				event.getMessage(),
				channel);
	}

	private String getPingResponse(String content) {
		if(content.contains("3.14")) {
			return "I like Pi";
		} else if(content.contains("curseforge") || content.contains("modrinth")) {
			return "CurseForge? Modrinth? Huh? Only thing I care about is <https://curseforge.com/minecraft/mc-mods/badoptimizations> ;)";
		} else if(content.contains("i fell")) {
			return "...from the light?";
		} else if(content.contains("wonder")) {
			return "https://tenor.com/view/wonder-effect-super-mario-wonder-mario-irony-talking-flower-mario-bros-meme-gif-4164277542385047547";
		} else if(content.contains("walls")) {
			return "https://tenor.com/view/im-in-your-walls-gif-25753367";
		} else if(content.contains("hair")) {
			return "Thanks for asking, I use dungeon-metal hair gel.";
		} else if(content.contains("everyone")) {
			if(content.contains("dont")) {
				return "Pfft, I'd never! Right...?";
			} else {
				return "Wanna ping everyone? Fill out the form at https://bit.ly/freevbucksomg/.";
			}
		}

		boolean hasHi = content.contains("hi");
		boolean hasHello = content.contains("hello");
		boolean hasDotDotDot = content.contains("...");
		boolean hasTheJunk = content.contains("!!?*%^@das@?!@");

		if(hasHi && hasDotDotDot && hasHello) {
			if(hasTheJunk) {
				return "" + System.currentTimeMillis();
			} else {
				return "!!?*%^@das@?!@";
			}
		} else if(!hasHi) {
			return "Hi!";
		} else if(!hasHello) {
			return "Hello!";
		} else {
			return "...";
		}
	}
}