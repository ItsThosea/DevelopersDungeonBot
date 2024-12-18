package me.thosea.developersdungeon.command;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.button.ButtonHandler;
import me.thosea.developersdungeon.util.AverageColorCounter;
import me.thosea.developersdungeon.util.Constants;
import me.thosea.developersdungeon.util.TeamRoleUtils;
import me.thosea.developersdungeon.util.TeamRoleUtils.TeamRolePair;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Icon.IconType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TeamCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("team", "Make, break or even modify a team role and its settings.")
				.addSubcommandGroups(new SubcommandGroupData("settings", "Your team's settings.")
						.addSubcommands(new SubcommandData("mentionable", "Change whether your team can be mentioned")
								.addOption(OptionType.BOOLEAN, "value", "Whether your team can be pinged", true))
						.addSubcommands(new SubcommandData("color", "Change your team color")
								.addOption(OptionType.STRING, "color", "Color (hex, R,G,B or \"random\")", true))
						.addSubcommands(new SubcommandData("rename", "Rename your team")
								.addOption(OptionType.STRING, "name", "New role name", true))
						.addSubcommands(new SubcommandData("icon", "Change your team role icon")
								.addOption(OptionType.ATTACHMENT, "icon", "New role icon", true))
						.addSubcommands(new SubcommandData("removeicon", "Remove your team role icon ")))
				.addSubcommands(new SubcommandData("create", "Make a new team")
						.addOption(OptionType.STRING, "name", "Role Name (you can change this later)", true)
						.addOption(OptionType.STRING, "color", "Color (can be changed later, hex, R,G,B or \"random\")"))
				.addSubcommands(new SubcommandData("delete", "Delete your team"))
				.addSubcommands(new SubcommandData("invite", "Invite somebody else")
						.addOption(OptionType.USER, "target", "Who to invite.", true))
				.addSubcommands(new SubcommandData("leave", "Leave your team"))
				.addSubcommands(new SubcommandData("transfer", "Change the team owner")
						.addOption(OptionType.USER, "target", "The new team owner.", true))
				.addSubcommands(new SubcommandData("info", "Display info about your team or the specified team.")
						.addOption(OptionType.MENTIONABLE, "target", "Target team or user. Leave blank to check your own team."))
				.addSubcommands(new SubcommandData("kick", "Kick somebody from the team")
						.addOption(OptionType.USER, "target", "Who to kick.", true))
				.addSubcommands(new SubcommandData("list", "What teams are on this server?"))
				.addSubcommands(new SubcommandData("listids", "Same as /team list, but with role IDs."));
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		long id = event.getChannelIdLong();
		if(Constants.Channels.BOTS > 0 && id != Constants.Channels.BOTS) {
			event.reply("You can only run this command in <#" + Constants.Channels.BOTS + ">!")
					.setEphemeral(true)
					.queue();
			return;
		}

		if("settings".equals(event.getSubcommandGroup())) {
			switch(event.getSubcommandName()) {
				case "mentionable" -> handleMentionable(member, event);
				case "rename" -> handleRename(member, event);
				case "color" -> handleSetColor(member, event);
				case "icon" -> handleIcon(member, event);
				case "removeicon" -> handleRemoveIcon(member, event);
				case null, default -> throw new IllegalStateException("Unexpected value: " + event.getSubcommandName());
			}
			return;
		}

		switch(event.getSubcommandName()) {
			case "create" -> handleCreate(member, event);
			case "delete" -> handleDelete(member, event);
			case "invite" -> handleRequest(member, event,
					"%s - you've been invited to join the team of %s by %s.",
					ButtonHandler.ID_JOIN_TEAM_ROLE, false);
			case "leave" -> handleLeave(member, event);
			case "transfer" -> handleRequest(member, event,
					"%s - you've been invited to take ownership of team %s by %s.",
					ButtonHandler.ID_TAKE_TEAM_OWNERSHIP, true);
			case "info" -> handleInfo(member, event);
			case "kick" -> handleKick(member, event);
			case "list", "listids" -> event.deferReply().queue(hook -> {
				handleList(member, hook, event.getSubcommandName().equals("listids"));
			});
			case null, default -> throw new IllegalStateException("Unexpected value: " + event.getSubcommandName());
		}
	}

	// credit: jab125
	private void handleMentionable(Member member, SlashCommandInteraction event) {
		TeamRolePair rolePair = TeamRoleUtils.getTeamRoles(member);

		if(rolePair.eitherNull()) {
			event.reply("You aren't an owner of a team!")
					.setEphemeral(true)
					.queue();
			return;
		}

		boolean mentionable = event.getOption("value", false, OptionMapping::getAsBoolean);

		event.deferReply().queue(hook -> {
			rolePair.baseRole().getManager().setMentionable(mentionable).queue();
			hook.editOriginal("Your team is now " + (mentionable ? "" : "not ") + "mentionable.").queue();
		});
	}

	// create mod reference??
	private void handleCreate(Member member, SlashCommandInteraction event) {
		if(TeamRoleUtils.hasTeamRole(member)) {
			event.reply("You're already in a team!").setEphemeral(true).queue();
			return;
		}

		String name = event.getOption("name", OptionMapping::getAsString);
		if(!TeamRoleUtils.isValidName(name)) {
			event.reply("Invalid name or it duplicates another role!").setEphemeral(true).queue();
			return;
		}

		Color color;
		String colorStr = event.getOption("color", OptionMapping::getAsString);

		if(colorStr != null) {
			color = Utils.parseColor(colorStr, event);
			if(color == null) return;
		} else {
			var random = ThreadLocalRandom.current();
			color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
		}

		event.deferReply().queue(hook -> Thread.ofVirtual().start(() -> {
					Role baseRole = Main.guild.createRole()
							.setName(name)
							.setColor(color)
							.setMentionable(true)
							.setPermissions(Set.of())
							.complete();
					Role ownerRole = Main.guild.createRole()
							.setPermissions()
							.setName(name + " (Owner)")
							.setColor(color)
							.complete();

					Main.guild.modifyRolePositions()
							.selectPosition(baseRole)
							.moveBelow(Main.teamRoleSandwichTop)
							.selectPosition(ownerRole)
							.moveBelow(Main.teamRoleSandwichTop)
							.queue();

					if(TeamRoleUtils.hasTeamRole(member)) {
						hook.editOriginal("You just got a team role!! Cheater!").queue();
						baseRole.delete().queue();
						ownerRole.delete().queue();
						return;
					}

					Main.guild.addRoleToMember(member, baseRole).queue();
					Main.guild.addRoleToMember(member, ownerRole).queue();

					String roles = baseRole.getAsMention() + " & " + ownerRole.getAsMention();

					hook.editOriginalEmbeds(new EmbedBuilder()
									.setColor(baseRole.getColorRaw())
									.setDescription("Your team roles have been made: " + roles +
											"\nInvite people with /team invite, " +
											"or change settings with /team settings.")
									.build())
							.setAllowedMentions(List.of())
							.queue();

					Utils.logMinor("%s made team %s: %s", member, name, roles);
				})
		);
	}

	private void handleDelete(Member member, SlashCommandInteraction event) {
		TeamRolePair rolePair = TeamRoleUtils.getTeamRoles(member);

		if(rolePair.eitherNull()) {
			event.reply("You aren't an owner of a team!")
					.setEphemeral(true)
					.queue();
			return;
		}

		event.reply("Are you sure you want to delete your team?")
				.setEphemeral(true)
				.setActionRow(Button.danger(ButtonHandler.ID_DELETE_TEAM_ROLE, "Delete"))
				.queue();
	}

	private void handleLeave(Member member, SlashCommandInteraction event) {
		TeamRolePair rolePair = TeamRoleUtils.getTeamRoles(member);

		if(rolePair.baseRole() == null) {
			event.reply("You aren't part of a team.").setEphemeral(true).queue();
			return;
		} else if(rolePair.ownerRole() != null) {
			event.reply("You are the owner of your team. Delete or transfer it first.")
					.setEphemeral(true)
					.queue();
			return;
		}

		event.reply("Are you sure you want to leave your team?")
				.setActionRow(Button.danger(ButtonHandler.ID_LEAVE_TEAM_ROLE, "Leave"))
				.setEphemeral(true)
				.queue();
	}

	private void handleRename(Member member, SlashCommandInteraction event) {
		TeamRolePair rolePair = TeamRoleUtils.getTeamRoles(member);
		if(rolePair.eitherNull()) {
			event.reply("You aren't an owner of a team!").setEphemeral(true).queue();
			return;
		}

		String name = event.getOption("name", OptionMapping::getAsString);
		if(!TeamRoleUtils.isValidName(name)) {
			event.reply("Invalid name or it duplicates another role!").setEphemeral(true).queue();
			return;
		}

		Utils.logMinor("%s renamed their team %s from %s to %s",
				member,
				rolePair.baseRole(),
				rolePair.baseRole().getName(),
				name);

		rolePair.baseRole().getManager().setName(name).queue();
		rolePair.ownerRole().getManager().setName(name + " (Owner)").queue();
		event.reply("Your team has been renamed to " + name + ".").queue();
	}

	private void handleSetColor(Member member, SlashCommandInteraction event) {
		TeamRolePair rolePair = TeamRoleUtils.getTeamRoles(member);
		if(rolePair.eitherNull()) {
			event.reply("You aren't an owner of a team!").setEphemeral(true).queue();
			return;
		}

		String colorStr = event.getOption("color", "", OptionMapping::getAsString);
		Color color = Utils.parseColor(colorStr, event);
		if(color == null) return;

		rolePair.baseRole().getManager().setColor(color).queue();
		rolePair.ownerRole().getManager().setColor(color).queue();

		colorStr = Utils.colorToString(color);

		event.replyEmbeds(new EmbedBuilder()
						.setColor(color)
						.setDescription("Changed your team color to " + colorStr)
						.build())
				.queue();
		Utils.logMinor("%s changed team color of %s to %s", member, rolePair.baseRole(), colorStr);
	}

	private void handleIcon(Member member, SlashCommandInteraction event) {
		if(TeamRoleUtils.getTeamRoles(member).eitherNull()) {
			event.reply("You aren't an owner of a team!").setEphemeral(true).queue();
			return;
		}

		Attachment attachment = event.getOption("icon", OptionMapping::getAsAttachment);
		assert attachment != null;

		IconType type;
		String extension = attachment.getFileExtension();
		if(extension == null || ((type = IconType.fromExtension(extension)) == IconType.UNKNOWN)) {
			event.reply("Invalid file extension.").setEphemeral(true).queue();
			return;
		}

		if(attachment.getSize() >= 41943040) { // 40 MiB
			event.reply("That file's too big!").setEphemeral(true).queue();
			return;
		}

		event.deferReply().queue(hook -> {
			attachment.getProxy().download().exceptionally(_ -> {
				hook.editOriginal("Failed to download the file. Was it deleted?").queue();
				return null;
			}).thenApply(stream -> {
				// recheck roles
				TeamRolePair pair = TeamRoleUtils.getTeamRoles(member);
				if(pair.eitherNull()) {
					hook.editOriginal("You aren't an owner of a team anymore!").queue();
					return stream;
				}

				try {
					var icon = Icon.from(stream, type);

					pair.baseRole().getManager().setIcon(icon)
							.and(pair.ownerRole().getManager().setIcon(icon))
							.queue(_ -> {
								hook.editOriginalEmbeds(new EmbedBuilder()
												.setColor(pair.baseRole().getColorRaw())
												.setThumbnail(attachment.getUrl())
												.setTitle("Your team role icon has been changed.")
												.build())
										.setAllowedMentions(List.of())
										.queue();

								Utils.logMinor("%s changed their team %s's role icon to %s",
										member, pair.baseRole(),
										attachment.getUrl());
							}, _ -> {
								hook.editOriginal("Failed to set icon. Is it smaller than 64x64 or over 256KB?" +
												"\n*(Protip: use https://discordicon.com/icons-editor)*")
										.setSuppressEmbeds(true)
										.queue();
							});
				} catch(IOException e) {
					hook.editOriginal("Failed to read image.").queue();
					return stream;
				}

				return stream;
			});
		});
	}

	private void handleRemoveIcon(Member member, SlashCommandInteraction event) {
		TeamRolePair pair = TeamRoleUtils.getTeamRoles(member);
		if(pair.eitherNull()) {
			event.reply("You aren't an owner of a team!").setEphemeral(true).queue();
			return;
		} else if(pair.baseRole().getIcon() == null) {
			event.reply("Your team role doesn't have an icon!").setEphemeral(true).queue();
			return;
		}

		pair.baseRole().getManager().setIcon((Icon) null)
				.and(pair.ownerRole().getManager().setIcon((Icon) null))
				.queue();
		event.reply("Your team role icon has been removed.").queue();

		Utils.logMinor("%s removed their team %s's role icon", member, pair.baseRole());
	}

	private void handleInfo(Member member, SlashCommandInteraction event) {
		IMentionable target = event.getOption("target", OptionMapping::getAsMentionable);
		Role base, owner;

		if(target == null || target.equals(member)) {
			base = TeamRoleUtils.getTeamRoles(member).baseRole();
			if(base == null) {
				event.reply("You aren't part of a team!").setEphemeral(true).queue();
				return;
			}
		} else {
			if(target instanceof Role role) {
				if(!TeamRoleUtils.isTeamRole(role)) {
					event.reply("That isn't a team role!").setEphemeral(true).queue();
					return;
				}

				base = role;
			} else if(target instanceof Member targetMember) {
				base = TeamRoleUtils.getTeamRoles(targetMember).baseRole();

				if(base == null) {
					event.reply("They aren't part of a team!").setEphemeral(true).queue();
					return;
				}
			} else {
				event.reply("Invalid mention type!").setEphemeral(true).queue();
				return;
			}
		}

		if(TeamRoleUtils.isTeamOwnerRole(base)) {
			owner = base;
			base = TeamRoleUtils.findBaseRole(owner);
			if(base == null) {
				event.reply("Could not find base team role.").setEphemeral(true).queue();
				return;
			}
		} else {
			owner = TeamRoleUtils.findOwnerRole(base);
			if(owner == null) {
				event.reply("Could not find team owner role.").setEphemeral(true).queue();
				return;
			}
		}

		final Role baseRole = base;
		final Role ownerRole = owner;
		event.deferReply().queue(hook -> {
			handleInfo(hook, baseRole, ownerRole);
		});
	}

	private static void handleInfo(InteractionHook hook, Role baseRole, Role ownerRole) {
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(baseRole.getColorRaw());
		embed.setTitle("Team Info - " + baseRole.getName());

		Color color = baseRole.getColor();
		String colorStr = color == null
				? "None"
				: Utils.colorToString(color);

		embed.appendDescription("Color: " + colorStr);
		embed.appendDescription("\nBase Role: " + baseRole.getAsMention());
		embed.appendDescription("\nOwner Role: " + ownerRole.getAsMention());

		if(baseRole.getIcon() != null) {
			embed.setThumbnail(baseRole.getIcon().getIconUrl());
		}

		Thread.ofVirtual().start(() -> {
			CompletableFuture<Long> future = TeamRoleUtils.getRoleOwner(ownerRole);
			Long ownerId = Utils.getSafe(future::join);

			if(ownerId != null) {
				embed.appendDescription("\nOwner: <@" + ownerId + ">");
			} else {
				embed.appendDescription("\nOwner: ???");
			}

			List<Member> members = Utils.getSafe(Main.guild.findMembersWithRoles(baseRole)::get);

			if(members == null) {
				embed.appendDescription("\nMembers: ???");
			} else if(members.size() == 1) {
				// Only owner
				embed.appendDescription("\nMembers: None");
			} else {
				StringBuilder builder = new StringBuilder();
				boolean isFirst = true;
				for(Member member : members) {
					if(ownerId != null && member.getIdLong() == ownerId) continue;
					if(isFirst) {
						isFirst = false;
					} else {
						builder.append(", ");
					}
					builder.append(member.getAsMention());
				}

				embed.appendDescription("\nMembers: " + builder);
			}

			hook.editOriginalEmbeds(embed.build()).setAllowedMentions(List.of()).queue();
		});
	}

	private void handleKick(Member member, SlashCommandInteraction event) {
		TeamRolePair pair = TeamRoleUtils.getTeamRoles(member);
		if(pair.eitherNull()) {
			event.reply("You aren't an owner of a team!").setEphemeral(true).queue();
			return;
		}

		Member target = event.getOption("target", OptionMapping::getAsMember);
		if(target == null) {
			event.reply("Invalid target.").setEphemeral(true).queue();
			return;
		} else if(target.equals(member)) {
			event.reply("You can't kick yourself, use /team leave instead.").setEphemeral(true).queue();
			return;
		} else if(!Utils.hasRole(target, pair.baseRole())) {
			event.reply("They aren't a part of your team!").setEphemeral(true).queue();
			return;
		}

		event.reply("Kicked " + target.getAsMention() + " from team " + pair.baseRole().getAsMention() + ".")
				.setAllowedMentions(List.of())
				.and(Main.guild.removeRoleFromMember(target, pair.baseRole()))
				.queue();

		Utils.logMinor("%s kicked %s from team %s", member, target, pair.baseRole());
	}

	private static final int TEAMS_PER_PAGE = 10;

	private void handleList(Member member, InteractionHook hook, boolean showId) {
		handleList(member.getId(), hook.editOriginal(""), WebhookMessageEditAction::queue, 1, showId);
	}

	public static <T extends MessageRequest<?>>
	void handleList(String userId, T hook, Consumer<T> sender, int page, boolean showId) {
		EmbedBuilder builder = new EmbedBuilder();
		builder.appendDescription("Team roles in " + Main.guild.getName());
		AverageColorCounter allPagesColor = new AverageColorCounter();

		List<Role> toSearch = getTeamRoles(allPagesColor);

		if(toSearch.isEmpty()) {
			hook.setContent("There are no team roles here.");
			sender.accept(hook);
			return;
		}

		int maxPage;
		boolean hasPages;
		if(toSearch.size() > TEAMS_PER_PAGE) {
			hasPages = true;

			List<List<Role>> allPages = Utils.splitList(toSearch, TEAMS_PER_PAGE);
			maxPage = allPages.size();
			page = Math.clamp(page - 1, 0, maxPage - 1);

			List<Role> currentPage = allPages.get(page).stream().toList();
			toSearch.clear();
			toSearch.addAll(currentPage);
			builder.appendDescription(" (Page " + (page + 1) + " of " + maxPage + ")");
		} else {
			hasPages = false;
			page = 0;
			maxPage = 0;
		}

		builder.appendDescription(":");

		Color allPageColorAvg = allPagesColor.average();
		builder.appendDescription("\nAverage Color (All Pages): " + Utils.colorToString(allPageColorAvg));

		if(!hasPages) {
			builder.setColor(allPageColorAvg);
		} else {
			AverageColorCounter pageColor = new AverageColorCounter();
			for(Role role : toSearch) {
				pageColor.addColor(role.getColor());
			}

			Color pageColorAvg = pageColor.average();
			builder.appendDescription("\nAverage Color (This Page): " + Utils.colorToString(pageColorAvg));

			AverageColorCounter embedColor = new AverageColorCounter();
			embedColor.addColor(allPageColorAvg);
			embedColor.addColor(pageColorAvg);
			builder.setColor(embedColor.average());
		}

		int finalPage = page;
		Thread.ofVirtual().start(() -> {
			for(Role role : toSearch) {
				Role ownerRole = TeamRoleUtils.findOwnerRole(role);
				Long ownerId = ownerRole == null
						? null
						: Utils.getSafe(TeamRoleUtils.getRoleOwner(ownerRole)::join);

				builder.appendDescription(makeTeamLine(role, ownerId, showId));
				hook.setEmbeds(builder.build());
				if(hasPages) {
					var prev = Button.primary(
							ButtonHandler.ID_TEAM_LIST_PAGE + "-" + finalPage + "-" + showId + "-" + userId,
							"< Previous Page");
					var next = Button.primary(
							ButtonHandler.ID_TEAM_LIST_PAGE + "-" + (finalPage + 2) + "-" + showId + "-" + userId,
							"Next Page >");

					if(finalPage == 0) {
						prev = prev.asDisabled();
					} else if(finalPage == maxPage - 1) {
						next = next.asDisabled();
					}
					hook.setActionRow(prev, next);
				}
			}

			sender.accept(hook);
		});
	}

	private static List<Role> getTeamRoles(AverageColorCounter totalColor) {
		List<Role> toSearch = new ArrayList<>();
		for(Role role : Main.guild.getRoles()) {
			if(!TeamRoleUtils.isTeamRole(role)) continue;
			if(TeamRoleUtils.isTeamOwnerRole(role)) continue;

			Role ownerRole = TeamRoleUtils.findOwnerRole(role);
			if(ownerRole == null) continue;

			totalColor.addColor(role.getColor());
			toSearch.add(role);
		}

		return toSearch;
	}

	private static String makeTeamLine(Role role, @Nullable Long ownerId, boolean showId) {
		StringBuilder line = new StringBuilder("\n" + role.getAsMention());
		if(showId) {
			line.append(" (").append(role.getId()).append(")");
		}

		line.append(" owned by ");
		if(ownerId == null) {
			line.append("???");
		} else {
			line.append("<@").append(ownerId).append(">");
		}

		if(ownerId != null && ownerId == 959062384419410011L) {
			line.append(" (<-- this team cool)");
		}

		return line.toString();
	}

	static final Map<Long, LongSet> requestCooldowns = new HashMap<>();

	private void handleRequest(Member member, SlashCommandInteraction event,
	                           String inviteMsg, String button,
	                           boolean isTransfer) {
		TeamRolePair pair = TeamRoleUtils.getTeamRoles(member);
		Member target = event.getOption("target", OptionMapping::getAsMember);

		if(target == null) {
			event.reply("Invalid target!").setEphemeral(true).queue();
			return;
		} else if(target.getUser().isBot()) {
			event.reply("You can't send requests to bots!").setEphemeral(true).queue();
			return;
		} else if(target.getIdLong() == member.getIdLong()) {
			event.reply("You can't target yourself!").setEphemeral(true).queue();
			return;
		} else if(pair.eitherNull()) {
			event.reply("You aren't an owner of a team!").setEphemeral(true).queue();
			return;
		} else if(!event.getChannel().asGuildMessageChannel().canTalk(target)) {
			event.reply("They cant access this channel!").setEphemeral(true).queue();
			return;
		} else checkTarget:{
			TeamRolePair targetPair = TeamRoleUtils.getTeamRoles(target);
			if(targetPair.baseRole() == null) break checkTarget;

			if(!targetPair.baseRole().equals(pair.baseRole())) {
				event.reply("They're already in a team!").setEphemeral(true).queue();
			} else {
				if(isTransfer) {
					break checkTarget;
				} else {
					event.reply("They're already in your team!").setEphemeral(true).queue();
				}
			}

			return;
		}

		LongSet cooldowns = requestCooldowns.get(member.getIdLong());
		if(cooldowns != null && cooldowns.contains(target.getIdLong())) {
			event.reply("You've already made a request to this person in the last minute!").setEphemeral(true).queue();
			return;
		}

		if(!Utils.isAdmin(member)) {
			requestCooldowns
					.computeIfAbsent(member.getIdLong(), _ -> new LongOpenHashSet())
					.add(target.getIdLong());
		}

		Utils.logMinor("%s made team %s request to %s for team %s", member, isTransfer ? "transfer" : "invite", target, pair.baseRole());

		event.reply(inviteMsg.formatted(target.getAsMention(), pair.baseRole(), member.getAsMention()))
				.setAllowedMentions(List.of(MentionType.USER))
				.setActionRow(
						Button.success(
								button + "-" + target.getId()
										+ "-" + member.getId()
										+ "-" + pair.baseRole().getId(),
								isTransfer ? "Take Ownership" : "Join"),
						Button.secondary(
								ButtonHandler.ID_DENY_TEAM_REQUEST
										+ "-" + (isTransfer ? "transfer" : "invite")
										+ "-" + target.getId()
										+ "-" + member.getId(),
								"Cancel request"))
				.queue(_ -> {
					Utils.doLater(TimeUnit.SECONDS, 60, () -> {
						requestCooldowns.computeIfPresent(member.getIdLong(), (_, set) -> {
							set.remove(target.getIdLong());
							return set.isEmpty() ? null : set;
						});
					});
				});
	}
}