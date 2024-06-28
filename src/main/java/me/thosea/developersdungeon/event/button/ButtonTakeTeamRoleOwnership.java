package me.thosea.developersdungeon.event.button;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.TeamRoleUtils;
import me.thosea.developersdungeon.util.TeamRoleUtils.TeamRolePair;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.List;

public class ButtonTakeTeamRoleOwnership implements ButtonHandler {
	@Override
	public String getId() {
		return ButtonHandler.ID_TAKE_TEAM_OWNERSHIP;
	}

	@Override
	public void handle(Member member, ButtonInteractionEvent event, String[] args) {
		if(args[1].equals(member.getId())) {
			event.reply("Tell who you invited to click the button to take ownership of the team.")
					.setEphemeral(true)
					.queue();
			return;
		} else if(!args[0].equals(member.getId())) {
			event.reply("This request isn't for you.").setEphemeral(true).queue();
			return;
		}

		Main.guild.retrieveMemberById(args[1]).queue(prevOwner -> {
			TeamRolePair pair = TeamRoleUtils.getTeamRoles(prevOwner);
			if(pair.eitherNull() || !pair.baseRole().getId().equals(args[2])) {
				event.reply("No longer valid.").setEphemeral(true).queue();
				return;
			}

			for(Role role : member.getRoles()) {
				if(TeamRoleUtils.isTeamRole(role) && role.getIdLong() != pair.baseRole().getIdLong()) {
					event.reply("You're on a different team. Leave that one first.")
							.setEphemeral(true)
							.queue();
					return;
				}
			}

			Main.guild.removeRoleFromMember(prevOwner, pair.ownerRole()).queue();
			Main.guild.addRoleToMember(member, pair.ownerRole()).queue();
			Main.guild.addRoleToMember(member, pair.baseRole()).queue();

			String log = String.format(
					"%s transferred team %s to %s.",
					prevOwner.getAsMention(),
					pair.baseRole().getAsMention(),
					member.getAsMention()
			);

			event.reply(log).setAllowedMentions(List.of()).queue();
			event.getMessage().delete().queue();
			Utils.logMinor(log);
		}, err -> {
			event.reply("No longer valid.").setEphemeral(true).queue();
		});
	}
}
