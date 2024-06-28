package me.thosea.developersdungeon.event.button;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.TeamRoleUtils;
import me.thosea.developersdungeon.util.TeamRoleUtils.TeamRolePair;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.List;

public class ButtonLeaveTeamRole implements ButtonHandler {
	@Override
	public String getId() {
		return ButtonHandler.ID_LEAVE_TEAM_ROLE;
	}

	@Override
	public void handle(Member member, ButtonInteractionEvent event, String[] args) {
		TeamRolePair rolePair = TeamRoleUtils.getTeamRoles(member);

		if(rolePair.baseRole() == null || rolePair.ownerRole() != null) {
			event.reply("No longer valid.").setEphemeral(true).queue();
			return;
		}

		Main.guild.removeRoleFromMember(member, rolePair.baseRole()).queue();
		event.reply("You are no longer a part of " + rolePair.baseRole().getAsMention() + ".")
				.setEphemeral(true)
				.setAllowedMentions(List.of())
				.queue();
		Utils.logMinor("%s left their team %s", member, rolePair.baseRole());
	}
}
