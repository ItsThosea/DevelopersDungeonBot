package me.thosea.developersdungeon.util;

import me.thosea.developersdungeon.Main;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class TeamRoleUtils {
	private TeamRoleUtils() {}

	public static boolean isTeamRole(Role role) {
		return role.compareTo(Main.teamRoleSandwichBottom) > 0 && role.compareTo(Main.teamRoleSandwichTop) < 0;
	}

	public static boolean hasTeamRole(Member member) {
		for(Role role : member.getRoles()) {
			if(TeamRoleUtils.isTeamRole(role)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isValidName(@Nullable String name) {
		if(name == null) return false;
		if(name.length() + " (Owner)".length() > 100) {
			return false;
		} else {
			name = name.toLowerCase(Locale.ENGLISH);
			return !name.contains("owner");
		}
	}

	public static boolean isTeamOwnerRole(Role role) {
		return role.getName().contains("(Owner)");
	}

	public static Role findBaseRole(Role owner) {
		if(!isTeamOwnerRole(owner)) return owner;

		String name = owner.getName().substring(
				0,
				owner.getName().length() - " (Owner)".length());

		for(Role role : Main.guild.getRoles()) {
			if(!isTeamRole(role)) continue;
			if(role.getName().equals(name)) {
				return role;
			}
		}

		return null;
	}

	public static Role findOwnerRole(Role base) {
		if(isTeamOwnerRole(base)) return base;

		String name = base.getName() + " (Owner)";
		for(Role role : Main.guild.getRoles()) {
			if(!isTeamRole(role)) continue;
			if(role.getName().equals(name)) {
				return role;
			}
		}

		return null;
	}

	public static TeamRolePair getTeamRoles(Member member) {
		Role ownerRole = null, baseRole = null;

		for(Role role : member.getRoles()) {
			if(!TeamRoleUtils.isTeamRole(role)) continue;

			if(TeamRoleUtils.isTeamOwnerRole(role)) {
				ownerRole = role;
			} else {
				baseRole = role;
			}
		}

		return new TeamRolePair(ownerRole, baseRole);
	}

	public record TeamRolePair(Role ownerRole, Role baseRole) {
		public boolean eitherNull() {
			return ownerRole == null || baseRole == null;
		}
	}

}
