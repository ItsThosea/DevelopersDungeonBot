package me.thosea.developersdungeon.button;

import me.thosea.developersdungeon.util.TeamRoleUtils;
import me.thosea.developersdungeon.util.TeamRoleUtils.TeamRolePair;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.List;

public class ButtonDeleteTeamRole implements ButtonHandler {
	@Override
	public String getId() {
		return ButtonHandler.ID_DELETE_TEAM_ROLE;
	}

	@Override
	public void handle(Member member, ButtonInteractionEvent event, String[] args) {
		TeamRolePair rolePair = TeamRoleUtils.getTeamRoles(member);
		Role baseRole = rolePair.baseRole();
		Role ownerRole = rolePair.ownerRole();

		if(baseRole == null || ownerRole == null) {
			event.reply("No longer valid.").setEphemeral(true).queue();
			return;
		}

		baseRole.delete().queue();
		ownerRole.delete().queue();
		event.reply(member.getAsMention() + " deleted their team " + baseRole.getName() + ".")
				.setAllowedMentions(List.of())
				.queue();

		Utils.logMinor("%s deleted their team %s (%s)", member, baseRole, baseRole.getName());
	}
}
