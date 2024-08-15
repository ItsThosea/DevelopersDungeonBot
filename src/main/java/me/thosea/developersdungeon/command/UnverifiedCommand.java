package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.Constants.Channels;
import me.thosea.developersdungeon.util.Constants.Roles;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public class UnverifiedCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("unverified", "List people here who haven't been verified yet.");
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		Role verifiedRole = Main.guild.getRoleById(Roles.VERIFIED);
		event.deferReply().setEphemeral(true).queue(hook -> {
			Main.guild.findMembers(user -> {
				return !Utils.hasRole(user, verifiedRole) && !Utils.isAdmin(user);
			}).onSuccess(list -> {
				if(list.isEmpty()) {
					hook.editOriginal("Everybody here is verified!").queue();
					return;
				}

				var channel = Main.guild.getTextChannelById(Channels.VERIFY_CHANNEL);
				if(channel == null) {
					handleList(hook, list, null, null);
				} else {
					channel.getHistory().retrievePast(100).queue(messages -> {
						handleList(hook, list, channel, messages);
					});
				}
			});
		});
	}

	private void handleList(InteractionHook hook, List<Member> list,
	                        TextChannel channel, List<Message> messages) {
		StringBuilder builder =
				new StringBuilder(
						"Unverified members in ").append(Main.guild.getName())
						.append(":");

		if(messages == null) {
			builder.append("\nWarning: No verify channel found.");
		}

		for(Member member : list) {
			String line = getLineFor(messages, member, channel);

			if(builder.length() + line.length() + 1 > Message.MAX_CONTENT_LENGTH) {
				builder.append("\nReached text limit.");
			} else {
				builder.append('\n').append(line);
			}
		}

		hook.editOriginal(builder.toString())
				.setAllowedMentions(List.of())
				.queue();
	}

	private String getLineFor(List<Message> messages, Member member, TextChannel channel) {
		if(messages == null) {
			return member.getAsMention();
		}

		List<Message> fromThem = messages
				.stream()
				.filter(msg -> msg.getAuthor().getIdLong() == member.getIdLong())
				.toList();

		String line = switch(fromThem.size()) {
			case 0 -> "No recent messages in " + channel.getAsMention();
			case 1 -> fromThem.get(0).getJumpUrl();
			case 2 -> "%s and %s".formatted(
					fromThem.get(0).getJumpUrl(),
					fromThem.get(1).getJumpUrl());
			case 3 -> "%s, %s and %s".formatted(
					fromThem.get(0).getJumpUrl(),
					fromThem.get(1).getJumpUrl(),
					fromThem.get(2).getJumpUrl());
			default -> "%s, %s, %s and more".formatted(
					fromThem.get(0).getJumpUrl(),
					fromThem.get(1).getJumpUrl(),
					fromThem.get(2).getJumpUrl());
		};

		return member.getAsMention() + " > " + line;
	}
}