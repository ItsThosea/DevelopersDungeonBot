package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.PChannelUtils;
import me.thosea.developersdungeon.util.TeamRoleUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.TargetType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideCreateEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideDeleteEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class PChannelListener extends ListenerAdapter {
	@Override
	public void onPermissionOverrideCreate(PermissionOverrideCreateEvent event) {
		if(!(event.getChannel() instanceof GuildMessageChannel channel)) return;
		if(!PChannelUtils.isPrivateChannel(channel)) return;

		getUpdater(channel, ActionType.CHANNEL_OVERRIDE_CREATE, member -> {
			handleNewOverride(event, channel, member);
		});
	}

	private void handleNewOverride(PermissionOverrideCreateEvent event, GuildMessageChannel channel, Member member) {
		PermissionOverride override = event.getPermissionOverride();

		if(handleEveryoneAdd(member, channel, override)) {
			return;
		}

		if(override.isRoleOverride()
				&& override.getRole() != null
				&& !TeamRoleUtils.isTeamRole(override.getRole())
				&& (member == null || !Utils.isAdmin(member))) {
			channel.sendMessage(getName(member) + " - you can't add non-team roles to your channel.")
					.setAllowedMentions(List.of())
					.queue();

			channel.getPermissionContainer().getManager()
					.removePermissionOverride(override.getIdLong())
					.queue();
			Utils.logMajor("%s tried to add permission override for non-team role %s in channel %s",
					member,
					override.getRole().getAsMention(),
					channel);
		} else {
			Utils.logMinor("%s added permission override for %s for channel %s",
					member,
					getTargetName(event.getPermissionOverride()),
					event.getChannel());
		}

		if(channel.getPermissionContainer()
				.getPermissionOverrides().size() > 6 // 1 less to account for the user themselves
				&& (member == null || !Utils.isAdmin(member))) {
			Utils.logMajor("%s added over 5 permission overrides to %s", member, channel);
		}
	}

	@Override
	public void onPermissionOverrideUpdate(PermissionOverrideUpdateEvent event) {
		if(!(event.getChannel() instanceof GuildMessageChannel channel)) return;
		if(!PChannelUtils.isPrivateChannel(channel)) return;

		getUpdater(channel, ActionType.CHANNEL_OVERRIDE_UPDATE, member -> {
			if(!handleEveryoneAdd(member, channel, event.getPermissionOverride())) {
				Utils.logMinor("%s updated permission override for %s for channel %s",
						member,
						getTargetName(event.getPermissionOverride()),
						event.getChannel());
			}
		});
	}

	@Override
	public void onPermissionOverrideDelete(PermissionOverrideDeleteEvent event) {
		if(!(event.getChannel() instanceof GuildMessageChannel channel)) return;
		if(!PChannelUtils.isPrivateChannel(channel)) return;

		getUpdater(channel, ActionType.CHANNEL_OVERRIDE_DELETE, member -> {
			if(!handleEveryoneAdd(member, channel, event.getPermissionOverride())) {
				Utils.logMinor("%s removed permission override for %s for channel %s",
						member,
						getTargetName(event.getPermissionOverride()),
						event.getChannel());
			}
		});
	}

	private String getTargetName(PermissionOverride override) {
		if(override.isRoleOverride()) {
			return "<@&" + override.getId() + ">";
		} else {
			return "<@" + override.getId() + ">";
		}
	}

	private boolean handleEveryoneAdd(Member member, GuildMessageChannel channel, PermissionOverride override) {
		if(!override.isRoleOverride()
				|| !Main.guild.getPublicRole().equals(override.getRole())
				|| (member != null && Utils.isAdmin(member))) {
			return false;
		}

		channel.getPermissionContainer().getManager().putPermissionOverride(
						Main.guild.getPublicRole(),
						null, // allowed
						Set.of(Permission.VIEW_CHANNEL)) // denied
				.queue();
		Utils.logMajor("%s tried to add permission override for @everyone in channel %s", member, channel);
		channel.sendMessage(getName(member) + " - you can't add @everyone to your channel.")
				.setAllowedMentions(List.of())
				.queue();
		return true;
	}

	private String getName(@Nullable Member member) {
		return member == null ? "???" : member.getAsMention();
	}

	private void getUpdater(Channel channel, ActionType action, Consumer<Member> handler) {
		OffsetDateTime timeMinimum = OffsetDateTime.now().minusSeconds(8);

		Thread.ofVirtual().start(() -> {
			List<AuditLogEntry> logs = Utils.getSafe(Main.guild.retrieveAuditLogs().type(action)::complete);
			if(logs == null) {
				handler.accept(null);
				return;
			}

			for(AuditLogEntry log : logs) {
				if(log.getTargetType() != TargetType.CHANNEL) continue;
				if(log.getTargetIdLong() != channel.getIdLong()) continue;

				if(log.getTimeCreated().isBefore(timeMinimum)) {
					// If that's the oldest one, then there probably wasn't a log for it.
					// Or the internet is just really delayed.
					break;
				}

				long id = log.getUserIdLong();

				if(id == Main.jda.getSelfUser().getIdLong()) {
					handler.accept(Main.guild.getSelfMember());
				} else {
					handler.accept(Utils.getSafe(Main.guild.retrieveMemberById(id)::complete));
				}
				return;
			}

			handler.accept(null);
		});
	}
}