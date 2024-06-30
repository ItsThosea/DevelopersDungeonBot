package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public class UnverifyCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("unverify", "Removes the verified role from a target.")
				.addOption(OptionType.USER, "target", "Who to unverify.", true);
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		Role requiredRole = Main.guild.getRoleById(1256763621053304863L);
		Role verifiedRole = Main.guild.getRoleById(1256188860296069132L);

		if(requiredRole == null || verifiedRole == null) {
			event.reply("Failed to find roles. This is a bug.")
					.setEphemeral(true)
					.queue();
			return;
		} else if(!member.getRoles().contains(requiredRole) && !Utils.isAdmin(member)) {
			event.reply("You don't have permission to do that!")
					.setEphemeral(true)
					.queue();
			return;
		}

		Member target = event.getOption("target", OptionMapping::getAsMember);
		if(target == null) {
			event.reply("Invalid user!").setEphemeral(true).queue();
		} else if(!target.getRoles().contains(verifiedRole)) {
			event.reply(target.getAsMention() + " is not verified!")
					.setEphemeral(true)
					.setAllowedMentions(List.of())
					.queue();
		} else {
			Main.guild.removeRoleFromMember(target, verifiedRole).queue(i_ -> {
				event.reply("Unverified " + target.getAsMention() + ".")
						.setEphemeral(true)
						.setAllowedMentions(List.of())
						.queue();

				Utils.logMinor("%s unverified %s", member, target);
			});
		}
	}
}
