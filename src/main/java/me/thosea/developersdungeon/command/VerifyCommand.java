package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.Constants;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public class VerifyCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("verify", "Verify a user.")
				.addOption(OptionType.USER, "target", "Who to verify.", true);
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		Role requiredRole = Main.guild.getRoleById(Constants.Roles.STAFF);
		Role verifiedRole = Main.guild.getRoleById(Constants.Roles.VERIFIED);

		if(requiredRole == null || verifiedRole == null) {
			event.reply("Failed to find roles. This is a bug.")
					.setEphemeral(true)
					.queue();
			return;
		} else if(!Utils.hasRole(member, requiredRole) && !Utils.isAdmin(member)) {
			event.reply("You don't have permission to do that!")
					.setEphemeral(true)
					.queue();
			return;
		}

		Member target = event.getOption("target", OptionMapping::getAsMember);
		if(target == null) {
			event.reply("Invalid user!").setEphemeral(true).queue();
		} else if(Utils.hasRole(member, verifiedRole)) {
			event.reply(target.getAsMention() + " is already verified!")
					.setEphemeral(true)
					.setAllowedMentions(List.of())
					.queue();
		} else {
			event.reply("Verified " + target.getAsMention() + ".")
					.setEphemeral(true)
					.setAllowedMentions(List.of())
					.queue();
			verify(member, target, verifiedRole);
		}
	}

	public static void verify(Member verifier, Member target, Role verifiedRole) {
		Main.guild.addRoleToMember(target, verifiedRole).queue(i_ -> {
			if(Main.generalChannel != null) {
				String msg = "Welcome to Developers Dungeon, " + target.getAsMention() + "!";
				if(Constants.Channels.INFORMATION_CHANNEL > 0) {
					msg += " Please check out <#" + Constants.Channels.INFORMATION_CHANNEL + ">!";
				}

				Main.generalChannel.sendMessage(msg).queue();
			}

			Utils.logMinor("%s verified %s", verifier, target);
		});
	}
}