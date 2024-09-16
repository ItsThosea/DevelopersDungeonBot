package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.util.TeamRoleUtils;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.entities.MemberImpl;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RoleRemoveEvent extends ListenerAdapter {
	@Override
	public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
		TeamRoleUtils.OWNER_CACHE.entrySet().removeIf(entry -> {
			return TeamRoleUtils.equalsSafe(entry.getValue(), event.getUser().getIdLong());
		});
	}

	@Override
	public void onRoleDelete(RoleDeleteEvent event) {
		TeamRoleUtils.OWNER_CACHE.remove(event.getRole().getIdLong());
	}

	@Override
	public void onGenericGuildMember(GenericGuildMemberEvent event) {
		Set<Role> roles = ((MemberImpl) event.getMember()).getRoleSet();

		roles.stream().filter(TeamRoleUtils::isTeamOwnerRole).forEach(role -> {
			var future = CompletableFuture.completedFuture(event.getMember().getIdLong());
			TeamRoleUtils.OWNER_CACHE.put(role.getIdLong(), future);
		});
	}

	@Override
	public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
		event.getRoles().stream().filter(TeamRoleUtils::isTeamOwnerRole).forEach(role -> {
			TeamRoleUtils.OWNER_CACHE.computeIfPresent(role.getIdLong(), (_, future) -> {
				return TeamRoleUtils.equalsSafe(future, event.getMember().getIdLong())
						? null
						: future;
			});
		});
	}
}