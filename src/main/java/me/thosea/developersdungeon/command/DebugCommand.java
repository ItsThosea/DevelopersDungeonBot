package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.event.ButtonListener;
import me.thosea.developersdungeon.util.Constants;
import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DebugCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("debug", "Debug action. Thosea-only!!!")
				.addOption(OptionType.INTEGER, "opcode", "opcode", true)
				.addOption(OptionType.STRING, "args", "args");
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		if(!Constants.ADMINS.contains(member.getIdLong())) {
			event.reply("You can't do that!")
					.setEphemeral(true)
					.queue();
			return;
		}

		int opcode = event.getOption("opcode", OptionMapping::getAsInt);

		String argsRaw = event.getOption("args", OptionMapping::getAsString);
		String[] args;
		if(argsRaw == null) {
			args = new String[0];
		} else {
			args = Stream.of(argsRaw.split(" "))
					.filter(Predicate.not(String::isBlank))
					.toArray(String[]::new);
		}

		if(opcode == 0) {
			handleOpcode0(event);
		} else if(opcode == 1) {
			handleOpcode1(event);
		} else if(opcode == 2) {
			event.deferReply().setEphemeral(true).queue(hook -> {
				handleOpcode2(hook, args);
			});
		} else if(opcode == 3) {
			event.deferReply()
					.setEphemeral(true)
					.queue(hook -> Main.guild.retrieveInvites().queue(list -> {
						handleOpcode3_4(hook, list);
					}));
		} else {
			event.reply("""
							Invalid opcodes. Opcodes:
							0: Resend state message
							1: Delete commission request forum post
							2: Toggle role (user, role)
							3: List invites (ephemeral)
							""")
					.setEphemeral(true)
					.queue();
		}
	}

	private void handleOpcode0(SlashCommandInteraction event) {
		var channel = event.getChannel();

		if(!ForumUtils.isCommissionRequest(channel)) {
			event.reply("Not a commission request channel").setEphemeral(true).queue();
			return;
		} else if(channel.getIdLong() != channel.getLatestMessageIdLong()) {
			event.reply("There are messages in this channel. Delete them first!").setEphemeral(true).queue();
			return;
		}

		ForumUtils.sendBotMessage(channel.asThreadChannel());
		event.reply("Sent bot message").setEphemeral(true).queue();
	}

	private void handleOpcode1(SlashCommandInteraction event) {
		var channel = event.getChannel();

		if(!ForumUtils.isCommissionRequest(channel)) {
			event.reply("Not a commission request channel").setEphemeral(true).queue();
			return;
		}

		ButtonListener.doDebugMessage(
				event,
				"Really delete this forum post completely?",
				false,
				id -> Button.danger(id, "Delete"),
				hook -> channel.delete().queue() // no need to respond
		);
	}

	private void handleOpcode2(InteractionHook hook, String[] args) {
		long targetId, roleId;
		try {
			String target = args[0];
			if(target.startsWith("<@")) {
				target = target.substring(2, target.length() - 1);
			}

			String targetRole = args[1];
			if(targetRole.startsWith("<@&")) {
				targetRole = targetRole.substring(3, targetRole.length() - 1);
			}

			targetId = Long.parseLong(target);
			roleId = Long.parseLong(targetRole);
		} catch(Exception ignored) {
			hook.editOriginal("Invalid args").queue();
			return;
		}

		Role role = Main.guild.getRoleById(roleId);
		if(role == null) {
			hook.editOriginal("No role found").queue();
			return;
		}

		Main.guild.retrieveMemberById(targetId).queue(member -> {
			boolean hadRole = Utils.hasRole(member, role);
			var request = !hadRole
					? Main.guild.addRoleToMember(member, role)
					: Main.guild.removeRoleFromMember(member, role);

			request.queue(
					i_ -> hook.editOriginal("Done (" + (!hadRole ? "GAVE" : "REMOVED") + ")").queue(),
					err -> hook.editOriginal("Failed: " + err).queue());
		}, err -> hook.editOriginal("No person found").queue());
	}

	private static void handleOpcode3_4(InteractionHook hook, List<Invite> list) {
		InviteLeaderboardCommand.makeResponse(hook, list);
	}
}
