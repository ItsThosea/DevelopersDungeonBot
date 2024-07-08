package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.command.VerifyCommand;
import me.thosea.developersdungeon.util.Constants.Channels;
import me.thosea.developersdungeon.util.Constants.Messages;
import me.thosea.developersdungeon.util.Constants.Roles;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.entities.MemberImpl;

public class AutoReactionListener extends ListenerAdapter {
	private static final Emoji YES = Emoji.fromUnicode("U+2705");
	private static final Emoji NO = Emoji.fromUnicode("U+274C");

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if(event.isWebhookMessage()) return;

		Member author = event.getMember();
		assert author != null; // never true
		if(author.getIdLong() == Main.jda.getSelfUser().getIdLong()) return;

		Message msg = event.getMessage();

		long id = event.getChannel().getIdLong();
		if(EchoMessageListener.lastEchoChannel == id) {
			EchoMessageListener.lastEchoChannel = -1;
			return;
		}

		boolean isVerifiedChannel = id == Channels.VERIFY_CHANNEL;
		boolean isSuggestionsChannel = id == Channels.SUGGESTIONS_CHANNEL;

		if(isSuggestionsChannel) {
			String name = author.getUser().getName();

			event.getChannel().retrieveMessageById(Messages.SUGGESTION_COUNT).queue(countMsg -> {
				int count = Integer.parseInt(countMsg.getContentRaw()) + 1;
				countMsg.editMessage("" + count).queue();
				msg.createThreadChannel("Suggestion #" + count + " - " + name).queue();
			}, err -> {
				Utils.logMinor("Warning: no suggestion count message in %s", event.getChannel());
				msg.createThreadChannel("Suggestion - " + name).queue();
			});
		} else {
			if(!isVerifiedChannel) return;
			if(hasRole(author, Roles.VERIFIED)) return;
			if(hasRole(author, Roles.STAFF)) return;
			if(Utils.isAdmin(author)) return;
		}

		msg.addReaction(YES).queue();
		msg.addReaction(NO).queue();
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if(event.getUserIdLong() == Main.jda.getSelfUser().getIdLong()) return;

		long id = event.getChannel().getIdLong();
		if(id != Channels.SUGGESTIONS_CHANNEL && id != Channels.VERIFY_CHANNEL) return;

		boolean isYes = YES.equals(event.getEmoji());
		boolean isNo = NO.equals(event.getEmoji());

		event.retrieveMember().queue(member -> {
			if(id == Channels.SUGGESTIONS_CHANNEL) {
				if(!Utils.isAdmin(member) && !isYes && !isNo) {
					revertReaction(event);
				}
				return;
			}

			if(!hasRole(member, Roles.STAFF) && !Utils.isAdmin(member)) {
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
							Utils.logMinor("%s denied verification for %s, kicking them", member, target);
						}
					}, err -> {});
		});
	}

	private void revertReaction(MessageReactionAddEvent event) {
		event.retrieveMessage().queue(msg -> {
			msg.removeReaction(event.getEmoji(), event.getUser()).queue();
		}, err -> {});
	}

	private boolean hasRole(Member member, long id) {
		return ((MemberImpl) member).getRoleSet().stream().anyMatch(role -> {
			return role.getIdLong() == id;
		});
	}
}
