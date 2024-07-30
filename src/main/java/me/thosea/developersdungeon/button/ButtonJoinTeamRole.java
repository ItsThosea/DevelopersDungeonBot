package me.thosea.developersdungeon.button;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.TeamRoleUtils;
import me.thosea.developersdungeon.util.TeamRoleUtils.TeamRolePair;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.List;

public class ButtonJoinTeamRole implements ButtonHandler {
	@Override
	public String getId() {
		return ButtonHandler.ID_JOIN_TEAM_ROLE;
	}

	@Override
	public void handle(Member member, ButtonInteractionEvent event, String[] args) {
		if(args[1].equals(member.getId())) {
			event.reply("Tell who you invited to click the button to join your team role.")
					.setEphemeral(true)
					.queue();
			return;
		} else if(!args[0].equals(member.getId())) {
			event.reply("This request isn't for you.").setEphemeral(true).queue();
			return;
		}

		long roleId = Long.parseLong(args[2]);
		TeamRolePair pair = TeamRoleUtils.getTeamRoles(member);

		if(pair.baseRole() != null) {
			String word = roleId == pair.baseRole().getIdLong() ? "the" : "a";
			event.reply("You're already in " + word + " team!").setEphemeral(true).queue();
			return;
		}

		Role role = Main.guild.getRoleById(roleId);

		if(role == null) {
			event.reply("The role was deleted.").setEphemeral(true).queue();
			return;
		}

		Main.guild.addRoleToMember(member, role).queue();

		event.deferEdit().queue(msg -> msg.deleteOriginal().queue());
		event.getChannel().sendMessage(member.getAsMention() + " joined team role " + role.getAsMention() + "!")
				.setAllowedMentions(List.of())
				.queue();

		Utils.logMinor("%s joined team %s as invited by %s", member, role, "<@" + args[1] + ">");
	}
}
