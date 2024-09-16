package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.command.DebugCommand;
import me.thosea.developersdungeon.util.Constants;
import me.thosea.developersdungeon.util.Constants.Channels;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class IrresistibleSuggestionMessageListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if(!(event.getChannel() instanceof ThreadChannel thread)) return;
		if(thread.getParentChannel().getIdLong() != Channels.VOTING) return;

		Message msg = event.getMessage();

		if(msg.getType() == MessageType.THREAD_STARTER_MESSAGE) return;
		if(msg.getAuthor().getIdLong() == Main.jda.getSelfUser().getIdLong()) return;
		if(Constants.ADMINS.contains(msg.getAuthor().getIdLong())) return;

		thread.retrieveParentMessage().queue(parentMsg -> {
			if(!DebugCommand.isIrresistiblePoll(parentMsg)) return;

			String text = "%s practiced free speech. Censorship success.\nMay Thosea be dictator over all."
					.formatted(msg.getAuthor().getAsMention());

			msg.delete()
					.and(thread.sendMessage(text).setAllowedMentions(List.of()))
					.queue();
		});
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		long id = event.getChannel().getIdLong();
		if(id == Channels.VOTING) {
			event.retrieveMessage().queue(msg -> {
				DebugCommand.handleReactionOnIrresistiblePoll(msg, msg, event);
			});
			return;
		}

		if(!(event.getChannel() instanceof ThreadChannel thread)) return;
		if(thread.getParentChannel().getIdLong() != Channels.VOTING) return;

		event.retrieveMessage().queue(msg -> {
			thread.retrieveParentMessage().queue(parentMsg -> {
				DebugCommand.handleReactionOnIrresistiblePoll(parentMsg, msg, event);
			});
		});
	}
}