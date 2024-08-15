package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.util.TeamRoleUtils;
import me.thosea.developersdungeon.util.TeamRoleUtils.TeamRolePair;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class LeaveListener extends ListenerAdapter {
	@Override
	public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
		if(event.getMember() == null) return;

		TeamRolePair pair = TeamRoleUtils.getTeamRoles(event.getMember());
		if(pair.eitherNull()) return;

		pair.baseRole().delete().queue();
		pair.ownerRole().delete().queue();

		Utils.logMinor("Team %s (%s) was deleted because the owner, %s, left the server.",
				pair.baseRole(),
				pair.baseRole().getName(),
				event.getMember());
	}
}