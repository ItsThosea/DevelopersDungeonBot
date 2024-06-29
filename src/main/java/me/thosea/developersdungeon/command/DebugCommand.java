package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.event.ButtonListener;
import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.TeamRoleUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class DebugCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("debug", "Debug action. Thosea-only!!!")
				.addOption(OptionType.INTEGER, "opcode", "opcode", true)
				.addOption(OptionType.STRING, "args", "args");
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		if(member.getIdLong() != 959062384419410011L) {
			event.reply("You're not Thosea!")
					.setEphemeral(true)
					.queue();
			return;
		}

		int opcode = event.getOption("opcode", OptionMapping::getAsInt);
		String argsRaw = event.getOption("args", OptionMapping::getAsString);
		String[] args = argsRaw == null ? new String[0] : argsRaw.trim().split(" ");

		if(opcode == 0) {
			handleOpcode0(event);
		} else if(opcode == 1) {
			handleOpcode1(event);
		} else if(opcode == 2) {
			event.deferReply().setEphemeral(true).queue(hook -> {
				handleOpcode2(hook, args);
			});
		} else if(opcode == 3) {
			handleOpcode4(event);
		} else {
			event.reply("Invalid opcode").setEphemeral(true).queue();
		}
	}

	private static void handleOpcode0(SlashCommandInteraction event) {
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

	private static void handleOpcode1(SlashCommandInteraction event) {
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

	private static void handleOpcode2(InteractionHook hook, String[] args) {
		long targetId;
		long roleId;
		try {
			String targetMention = args[0].substring(2, args[0].length() - 1);
			String targetRole = args[1].substring(3, args[1].length() - 1);

			targetId = Long.parseLong(targetMention);
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
			Main.guild.addRoleToMember(member, role).queue(
					i_ -> hook.editOriginal("Done").queue(),
					err -> hook.editOriginal("Failed: " + err).queue()
			);
		}, err -> hook.editOriginal("Mo person found").queue());
	}

	private static void handleOpcode4(SlashCommandInteraction event) {
		StringBuilder builder = new StringBuilder("Roles:");
		for(Role role : Main.guild.getRoles()) {
			builder.append('\n');
			builder.append(role.getAsMention()).append(": ").append(TeamRoleUtils.isTeamRole(role));
		}

		event.reply(builder.toString()).setEphemeral(true).queue();
	}

}
