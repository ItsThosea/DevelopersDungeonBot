package me.thosea.developersdungeon.util;

import me.thosea.developersdungeon.Main;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.internal.entities.MemberImpl;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class TeamRoleUtils {
	private TeamRoleUtils() {}

	public static final Map<Long, CompletableFuture<Long>> OWNER_CACHE = Collections.synchronizedMap(new HashMap<>());

	public static boolean isTeamRole(Role role) {
		return role.compareTo(Main.teamRoleSandwichBottom) > 0 && role.compareTo(Main.teamRoleSandwichTop) < 0;
	}

	public static boolean hasTeamRole(Member member) {
		for(Role role : ((MemberImpl) member).getRoleSet()) {
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
			if(name.contains("owner")) {
				return false;
			}

			for(Role role : Main.guild.getRoles()) {
				if(role.getName().equalsIgnoreCase(name)) {
					return false;
				}
			}

			return true;
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

		for(Role role : ((MemberImpl) member).getRoleSet()) {
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

	public static CompletableFuture<Long> getRoleOwner(Role role) {
		return OWNER_CACHE.compute(role.getIdLong(), (_, existing) -> {
			if(existing != null
					&& !existing.isCompletedExceptionally()
					&& (!existing.isDone() || existing.getNow(-1L) != null)) {
				return existing;
			}

			CompletableFuture<Long> future = new CompletableFuture<>();

			Main.guild.findMembersWithRoles(role).setTimeout(15, TimeUnit.SECONDS).onSuccess(list -> {
				future.complete(list.size() == 1 ? list.getFirst().getIdLong() : null);
			}).onError(err -> {
				System.err.println("Error getting team owner");
				err.printStackTrace();
				future.completeExceptionally(err);
			});

			return future;
		});
	}
}