package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.command.VerifyCommand;
import me.thosea.developersdungeon.util.Constants.Channels;
import me.thosea.developersdungeon.util.Constants.Roles;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ReactionListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if(event.isWebhookMessage()) return;

		Member author = event.getMember();
		if(author == null) return;
		if(author.getIdLong() == Main.jda.getSelfUser().getIdLong()) return;

		Message msg = event.getMessage();
		if(msg.getType() != MessageType.DEFAULT) return;

		long id = event.getChannel().getIdLong();
		if(EchoMessageListener.lastEchoChannel == id) {
			EchoMessageListener.lastEchoChannel = -1;
			return;
		}

		if(id == Channels.VERIFY) {
			if(Utils.hasRole(author, Roles.VERIFIED)) return;
			if(Utils.hasRole(author, Roles.STAFF)) return;
			if(Utils.isAdmin(author)) return;
		} else if(id != Channels.SUGGESTIONS) {
			return;
		}

		msg.addReaction(Utils.EMOJI_YES).queue();
		msg.addReaction(Utils.EMOJI_NO).queue();
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if(event.getUserIdLong() == Main.jda.getSelfUser().getIdLong()) return;

		long id = event.getChannel().getIdLong();
		if(id != Channels.SUGGESTIONS && id != Channels.VERIFY) return;

		boolean isYes = Utils.EMOJI_YES.equals(event.getEmoji());
		boolean isNo = Utils.EMOJI_NO.equals(event.getEmoji());

		event.retrieveMember().queue(member -> {
			if(id == Channels.SUGGESTIONS) {
				if(!Utils.isAdmin(member) && !isYes && !isNo) {
					revertReaction(event);
				}
				return;
			}

			if(!Utils.hasRole(member, Roles.STAFF) && !Utils.isAdmin(member)) {
				if(isYes || isNo) {
					revertReaction(event);
				}
				return;
			}

			event.retrieveMessage()
					.flatMap(msg -> Main.guild.retrieveMember(msg.getAuthor()))
					.queue(target -> {
						Role verifiedRole = Main.guild.getRoleById(Roles.VERIFIED);

						if(Utils.hasRole(target, verifiedRole)) return;
						if(Utils.isAdmin(target)) return;

						if(isYes) {
							VerifyCommand.verify(member, target, verifiedRole);
						} else if(isNo) {
							target.kick().reason("Staff denied verification").queue();
							Utils.logMajor("%s denied verification for %s, kicking them", member, target);
						}
					}, _ -> {});
		});
	}

	private void revertReaction(MessageReactionAddEvent event) {
		event.retrieveMessage().queue(msg -> {
			msg.removeReaction(event.getEmoji(), event.getUser()).queue();
		}, _ -> {});
	}
}