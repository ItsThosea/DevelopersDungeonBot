package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.command.VerifyCommand;
import me.thosea.developersdungeon.util.Constants;
import me.thosea.developersdungeon.util.Constants.Channels;
import me.thosea.developersdungeon.util.Constants.Emojis;
import me.thosea.developersdungeon.util.Constants.Roles;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class AutoReactionListener extends ListenerAdapter {
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
		if(id == Channels.VOTING) {
			if(event.getMessageAuthorIdLong() == Main.jda.getSelfUser().getIdLong()
					&& event.getUserIdLong() != Main.jda.getSelfUser().getIdLong()
					&& !IRRESISTIBLE_REACTIONS.contains(event.getEmoji())
					&& !Constants.ADMINS.contains(event.getUserIdLong())) {
				handleVotingReaction(event);
			}

			return;
		}
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
							Utils.logMinor("%s denied verification for %s, kicking them", member, target);
						}
					}, _ -> {});
		});
	}

	private static final List<Emoji> IRRESISTIBLE_REACTIONS = List.of(
			Emoji.fromUnicode("U+2705"), // green checkmark
			Emoji.fromUnicode("U+1F44D"), // thumbs up
			Emoji.fromUnicode("U+1F525"), // fire
			Emoji.fromUnicode("U+1F64B"), // person raising hand
			Emojis.YEAH,
			Emoji.fromUnicode("U+2764"), // heart
			Emoji.fromUnicode("U+1F49F"), // heart decoration
			Emoji.fromUnicode("U+1FAF6"), // heart hands
			Emoji.fromUnicode("U+1F497"), // heart pulse
			Emoji.fromUnicode("U+1F4AF"), // 100
			Emoji.fromUnicode("U+1F44C"), // ok hand
			Emoji.fromUnicode("U+1F44F"), // clap
			Emoji.fromUnicode("U+1F1FE"), // Y
			Emoji.fromUnicode("U+1F1EA"), // E
			Emoji.fromUnicode("U+1F1F8"), // S
			Emoji.fromUnicode("U+203C"), // !
			Emojis.KEOIKI
	);

	private void handleVotingReaction(MessageReactionAddEvent event) {
		event.retrieveMessage().queue(msg -> {
			if(msg.getPoll() == null) return;
			if(msg.getPoll().getAnswers().size() != 2) return;
			if(!Utils.EMOJI_SMILE.equals(msg.getPoll().getAnswers().get(1).getEmoji())) {
				// not "irresistible"
				return;
			}

			msg.removeReaction(event.getEmoji(), event.getUser()).queue();
			if(msg.getReactions().size() - 1 >= 20) return; // max limit

			List<? extends Emoji> reacted = msg.getReactions().stream().map(MessageReaction::getEmoji).toList();
			for(Emoji emoji : IRRESISTIBLE_REACTIONS) {
				if(!reacted.contains(emoji)) {
					msg.addReaction(emoji).queue();
					break;
				}
			}
		});
	}

	private void revertReaction(MessageReactionAddEvent event) {
		event.retrieveMessage().queue(msg -> {
			msg.removeReaction(event.getEmoji(), event.getUser()).queue();
		}, _ -> {});
	}
}