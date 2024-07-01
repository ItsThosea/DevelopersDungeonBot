package me.thosea.developersdungeon.command;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.event.button.ButtonHandler;
import me.thosea.developersdungeon.util.TeamRoleUtils;
import me.thosea.developersdungeon.util.TeamRoleUtils.TeamRolePair;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TeamCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("team", "Make or break (or even modify) a team role and its color.")
				.addSubcommands(new SubcommandData("create", "Make a new team")
						.addOption(OptionType.STRING, "name", "Role Name (you can change this later)", true)
						.addOption(OptionType.STRING, "color", "Color (can be changed later, R,G,B or \"random\")"))
				.addSubcommands(new SubcommandData("delete", "Delete your team"))
				.addSubcommands(new SubcommandData("invite", "Invite somebody else")
						.addOption(OptionType.USER, "target", "Who to invite.", true))
				.addSubcommands(new SubcommandData("leave", "Leave your team"))
				.addSubcommands(new SubcommandData("rename", "Rename your team")
						.addOption(OptionType.STRING, "name", "New role name", true))
				.addSubcommands(new SubcommandData("setcolor", "Change your team color")
						.addOption(OptionType.STRING, "color", "Color (R,G,B or \"random\")", true))
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
		if(event.getChannel().getIdLong() != 1254942520920506369L) {
			event.reply("You can only run this command in <#1254942520920506369>!")
					.setEphemeral(true)
					.queue();
			return;
		}

		switch(event.getSubcommandName()) {
			case "create" -> handleCreate(member, event);
			case "delete" -> handleDelete(member, event);
			case "invite" -> handleRequest(member, event,
					"%s - you've been invited to join the team of %s by %s.",
					ButtonHandler.ID_JOIN_TEAM_ROLE, false);
			case "leave" -> handleLeave(member, event);
			case "rename" -> handleRename(member, event);
			case "setcolor" -> handleSetColor(member, event);
			case "transfer" -> handleRequest(member, event,
					"%s - you've been invited to take ownership of team %s by %s.",
					ButtonHandler.ID_TAKE_TEAM_OWNERSHIP, true);
			case "info" -> handleInfo(member, event);
			case "kick" -> handleKick(member, event);
			case "list", "listids" -> event.deferReply().queue(hook -> {
				handleList(hook, event.getSubcommandName().equals("listids"));
			});
			default -> throw new IllegalStateException("Unexpected value: " + event.getSubcommandName());
		}
	}

	// create mod reference??
	private void handleCreate(Member member, SlashCommandInteraction event) {
		if(TeamRoleUtils.hasTeamRole(member)) {
			event.reply("You're already in a team!").setEphemeral(true).queue();
			return;
		}

		String name = event.getOption("name", OptionMapping::getAsString);
		if(!TeamRoleUtils.isValidName(name)) {
			event.reply("Invalid name!").setEphemeral(true).queue();
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

		event.deferReply().queue(hook -> {
			var baseRequest = Main.guild
					.createRole()
					.setName(name)
					.setColor(color)
					.setMentionable(true)
					.setPermissions(Set.of());

			baseRequest.queue(baseRole -> {
				var ownerRequest = Main.guild.createRole()
						.setPermissions()
						.setName(name + " (Owner)")
						.setColor(color);

				ownerRequest.queue(ownerRole -> {
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

					hook.editOriginal("Your team roles have been made: " + roles +
									"\nInvite people with /team invite, " +
									"or change the color with /team setcolor")
							.setAllowedMentions(List.of())
							.queue();

					Utils.logMinor("%s made team %s: %s", member, name, roles);
				});
			});
		});
	}

	private void handleDelete(Member member, SlashCommandInteraction event) {
		TeamRolePair rolePair = TeamRoleUtils.getTeamRoles(member);

		if(rolePair.eitherNull()) {
			event.reply("You aren't an owner of a team.")
					.setEphemeral(true)
					.queue();
			return;
		}

		event.reply("Are you sure you want to delete your team role?")
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
			event.reply("You aren't an owner of a team.").setEphemeral(true).queue();
			return;
		}

		String name = event.getOption("name", OptionMapping::getAsString);
		if(!TeamRoleUtils.isValidName(name)) {
			event.reply("Invalid name!").setEphemeral(true).queue();
			return;
		}

		Utils.logMinor("%s renamed their team %s from %s to %s",
				member,
				rolePair.baseRole(),
				rolePair.baseRole().getName(),
				name);

		rolePair.baseRole().getManager()
				.setName(name + " (Team)")
				.queue();
		rolePair.ownerRole().getManager()
				.setName(name + " (Team Owner)")
				.queue();
		event.reply("Your team has been renamed.").queue();
	}

	private void handleSetColor(Member member, SlashCommandInteraction event) {
		TeamRolePair rolePair = TeamRoleUtils.getTeamRoles(member);
		if(rolePair.eitherNull()) {
			event.reply("You aren't an owner of a team.").setEphemeral(true).queue();
			return;
		}

		String colorStr = event.getOption("color", OptionMapping::getAsString);
		Color color = Utils.parseColor(colorStr, event);
		if(color == null) return;

		rolePair.baseRole().getManager().setColor(color).queue();
		rolePair.ownerRole().getManager().setColor(color).queue();

		colorStr = Utils.colorToString(color);

		event.reply("Changed your team color to " + colorStr).queue();
		Utils.logMinor("%s changed team color of %s to %s", member, rolePair.baseRole(), colorStr);
	}

	private void handleInfo(Member member, SlashCommandInteraction event) {
		IMentionable target = event.getOption("target", OptionMapping::getAsMentionable);
		Role base;
		Role owner;

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
		event.deferReply()
				.setEphemeral(event.getChannel().getIdLong() != 1254942520920506369L)
				.queue(hook -> {
					handleInfo(hook, baseRole, ownerRole);
				});
	}

	// Think you've seen cursed?
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

		BiConsumer<List<Member>, Throwable> ownerFindHandler = (ownerList, err1) -> {
			if(err1 != null || ownerList == null || ownerList.isEmpty()) {
				embed.appendDescription("\nOwner: ???");
			} else {
				embed.appendDescription("\nOwner: " + ownerList.getFirst().getAsMention());
			}

			BiConsumer<List<Member>, Throwable> baseFindHandler = (baseList, err) -> {
				if(baseList != null && ownerList != null && !ownerList.isEmpty()) {
					baseList.remove(ownerList.getFirst());
				}

				if(err != null || baseList == null) {
					embed.appendDescription("\nMembers: ???");
				} else if(baseList.isEmpty()) {
					embed.appendDescription("\nMembers: None");
				} else {
					StringBuilder builder = new StringBuilder();
					boolean isFirst = true;
					for(Member m : baseList) {
						if(isFirst) {
							isFirst = false;
						} else {
							builder.append(", ");
						}
						builder.append(m.getAsMention());
					}

					embed.appendDescription("\nMembers: " + builder);
				}

				hook.editOriginalEmbeds(embed.build()).setAllowedMentions(List.of()).queue();
			};


			Main.guild.findMembersWithRoles(baseRole)
					.onSuccess(list -> baseFindHandler.accept(list, null))
					.onError(err -> baseFindHandler.accept(null, err));
		};

		Main.guild.findMembersWithRoles(ownerRole)
				.onSuccess(list -> ownerFindHandler.accept(list, null))
				.onError(err -> ownerFindHandler.accept(null, err));
	}

	private void handleKick(Member member, SlashCommandInteraction event) {
		TeamRolePair pair = TeamRoleUtils.getTeamRoles(member);
		if(pair.eitherNull()) {
			event.reply("You aren't an owner of a team.").setEphemeral(true).queue();
			return;
		}

		Member target = event.getOption("target", OptionMapping::getAsMember);
		if(target == null) {
			event.reply("Invalid target.").setEphemeral(true).queue();
			return;
		} else if(target.equals(member)) {
			event.reply("You can't kick yourself, use /team leave instead.").setEphemeral(true).queue();
			return;
		} else if(!target.getRoles().contains(pair.baseRole())) {
			event.reply("They aren't a part of your team role.").setEphemeral(true).queue();
			return;
		}

		Main.guild.removeRoleFromMember(target, pair.baseRole()).queue();
		event.reply("Kicked " + target.getAsMention() + " from team " + pair.baseRole().getAsMention() + ".")
				.setAllowedMentions(List.of())
				.queue();

		Utils.logMinor("%s kicked %s from team %s", member, target, pair.baseRole());
	}

	private void handleList(InteractionHook hook, boolean showId) {
		EmbedBuilder builder = new EmbedBuilder();
		builder.appendDescription("Team Roles in Developers Dungeon: ");
		List<Pair<Role, Consumer<Member>>> toSearch = new ArrayList<>();

		int roles = 0;
		int avgRed = 0;
		int avgGreen = 0;
		int avgBlue = 0;

		for(Role role : Main.guild.getRoles()) {
			if(!TeamRoleUtils.isTeamRole(role)) continue;
			if(TeamRoleUtils.isTeamOwnerRole(role)) continue;

			Role ownerRole = TeamRoleUtils.findOwnerRole(role);
			if(ownerRole == null) continue;

			if(role.getColor() != null) {
				roles++;
				avgRed += role.getColor().getRed();
				avgGreen += role.getColor().getGreen();
				avgBlue += role.getColor().getBlue();
			}

			toSearch.add(Pair.of(ownerRole, member -> {
				builder.appendDescription("\n" + role.getAsMention());
				if(showId) {
					builder.appendDescription(" (" + role.getId() + ")");
				}

				String owner = member == null ? "???" : member.getAsMention();
				builder.appendDescription(" owned by " + owner);
			}));
		}

		if(toSearch.isEmpty()) {
			hook.editOriginal("There are no team roles empty.").queue();
			return;
		}

		Color color = new Color(avgRed / roles, avgGreen / roles, avgBlue / roles);
		builder.setColor(color);
		builder.appendDescription("\nAverage Color: " + Utils.colorToString(color));

		AtomicInteger done = new AtomicInteger();
		for(Pair<Role, Consumer<Member>> pair : toSearch) {
			Main.guild.findMembersWithRoles(pair.first())
					.onSuccess(list -> {
						int next = done.incrementAndGet();
						if(next == 0) {
							return; // -1
						} else {
							pair.second().accept(list.size() != 1 ? null : list.getFirst());
							if(next != toSearch.size()) return; // waiting on other requests
						}

						hook.editOriginalEmbeds(builder.build()).queue();
					})
					.onError(err -> {
						hook.editOriginal("Error: " + err).queue();
						System.out.println("Error while getting team members ");
						err.printStackTrace();
					});
		}
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
			event.reply("You can't invite bots!").setEphemeral(true).queue();
			return;
		} else if(target.getIdLong() == member.getIdLong()) {
			event.reply("You can't target yourself!").setEphemeral(true).queue();
			return;
		} else if(pair.eitherNull()) {
			event.reply("You aren't an owner of a team!").setEphemeral(true).queue();
			return;
		} else if(!isTransfer) {
			TeamRolePair targetPair = TeamRoleUtils.getTeamRoles(target);
			if(targetPair.baseRole() != null) {
				if(targetPair.baseRole().equals(pair.baseRole())) {
					event.reply("They're already in your team!").setEphemeral(true).queue();
				} else {
					event.reply("They're already in a team!").setEphemeral(true).queue();
				}

				return;
			}
		} else if(!event.getChannel().asGuildMessageChannel().canTalk(target)) {
			event.reply("They cant access this channel!").setEphemeral(true).queue();
			return;
		}

		LongSet cooldowns = requestCooldowns.get(member.getIdLong());
		if(cooldowns != null && cooldowns.contains(target.getIdLong())) {
			event.reply("You've already made a request to this person in the last 30 seconds.")
					.setEphemeral(true)
					.queue();
			return;
		}

		requestCooldowns.computeIfAbsent(member.getIdLong(), i_ -> new LongOpenHashSet())
				.add(target.getIdLong());

		Utils.logMinor("%s made team role %s request to %s for team %s",
				member,
				isTransfer ? "transfer" : "invite",
				target, pair.baseRole());

		long deleteTime = (System.currentTimeMillis() + 30 * 1000) / 1000L;
		event.reply(String.format(
						inviteMsg + "\n*(this message will auto delete in <t:" + deleteTime + ":R>)*",
						target.getAsMention(),
						pair.baseRole(),
						member.getAsMention()
				))
				.setAllowedMentions(List.of(MentionType.USER))
				.setActionRow(Button.success(
						button + "-" + target.getId()
								+ "-" + member.getId()
								+ "-" + pair.baseRole().getId(),
						isTransfer ? "Take Ownership" : "Join"))
				.queue(msg -> {
					Utils.doLater(TimeUnit.SECONDS, 30, () -> {
						requestCooldowns.computeIfPresent(member.getIdLong(), (i_, set) -> {
							set.remove(target.getIdLong());
							return set.isEmpty() ? null : set;
						});
						msg.deleteOriginal().queue(i_ -> {}, err -> {});
					});
				});
	}
}
