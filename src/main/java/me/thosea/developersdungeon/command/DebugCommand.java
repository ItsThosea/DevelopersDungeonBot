package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.TeamRoleUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class DebugCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("debug", "Debug action. Thosea-only!!!")
				.addOption(OptionType.INTEGER, "opcode", "Opcode", true);
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

		if(opcode == 0) {
			// I needed to remove my own permissions a few times so to give them back:
			Main.guild.addRoleToMember(member, Main.guild.getRoleById(1255193480062570578L))
					.reason("Debug command")
					.queue();
			event.reply("Gave you the role").setEphemeral(true).queue();
		} else if(opcode == 1) {
			// And to remove them
			Main.guild.removeRoleFromMember(member, Main.guild.getRoleById(1255193480062570578L))
					.reason("Debug command")
					.queue();
			event.reply("Removed the role").setEphemeral(true).queue();
		} else if(opcode == 2) {
			StringBuilder builder = new StringBuilder();
			boolean isFirst = true;

			for(Role role : Main.guild.getRoles()) {
				if(isFirst) {
					isFirst = false;
				} else {
					builder.append('\n');
				}

				builder.append(role.getAsMention()).append(": ").append(TeamRoleUtils.isTeamRole(role));
			}

			event.reply(builder.toString()).setEphemeral(true).queue();
		} else {
			event.reply("Invalid opcode").setEphemeral(true).queue();
		}
	}
}
