package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.event.ButtonListener;
import me.thosea.developersdungeon.util.Constants;
import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.TeamRoleUtils;
import me.thosea.developersdungeon.util.TeamRoleUtils.TeamRolePair;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DebugCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("debug", "Debug action. Thosea-only!!!")
				.addOption(OptionType.INTEGER, "opcode", "opcode", true)
				.addOption(OptionType.STRING, "args", "args");
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		if(!Constants.ADMINS.contains(member.getIdLong())) {
			event.reply("You can't do that!")
					.setEphemeral(true)
					.queue();
			return;
		}

		int opcode = event.getOption("opcode", OptionMapping::getAsInt);
		String argsRaw = event.getOption("args", OptionMapping::getAsString);
		String[] args = argsRaw == null ? new String[0] : argsRaw.trim().split(" ");

		if(opcode == 0) {
			handleOpcode0(event);
		} else if(opcode == 1) {
			handleOpcode1(event);
		} else if(opcode == 2) {
			event.deferReply().setEphemeral(true).queue(hook -> {
				handleOpcode2(hook, args);
			});
		} else if(opcode == 3 || opcode == 4) {
			event.deferReply()
					.setEphemeral(opcode == 4)
					.queue(hook -> Main.guild.retrieveInvites().queue(list -> {
						handleOpcode3_4(hook, list);
					}));
		} else {
			event.reply("""
							Invalid opcodes. Opcodes:
							0: Resend state message
							1: Delete commission request forum post
							2: Toggle role (user, role)
							3: List invites
							4: List invites (ephemeral)
							""")
					.setEphemeral(true)
					.queue();
		}
	}

	private void handleOpcode0(SlashCommandInteraction event) {
		var channel = event.getChannel();

		if(!ForumUtils.isCommissionRequest(channel)) {
			event.reply("Not a commission request channel").setEphemeral(true).queue();
			return;
		} else if(channel.getIdLong() != channel.getLatestMessageIdLong()) {
			event.reply("There are messages in this channel. Delete them first!").setEphemeral(true).queue();
			return;
		}

		ForumUtils.sendBotMessage(channel.asThreadChannel());
		event.reply("Sent bot message").setEphemeral(true).queue();
	}

	private void handleOpcode1(SlashCommandInteraction event) {
		var channel = event.getChannel();

		if(!ForumUtils.isCommissionRequest(channel)) {
			event.reply("Not a commission request channel").setEphemeral(true).queue();
			return;
		}

		ButtonListener.doDebugMessage(
				event,
				"Really delete this forum post completely?",
				false,
				id -> Button.danger(id, "Delete"),
				hook -> channel.delete().queue() // no need to respond
		);
	}

	private void handleOpcode2(InteractionHook hook, String[] args) {
		long targetId, roleId;
		try {
			String target = args[0];
			if(target.startsWith("<@")) {
				target = target.substring(2, target.length() - 1);
			}

			String targetRole = args[1];
			if(targetRole.startsWith("<@&")) {
				targetRole = targetRole.substring(3, targetRole.length() - 1);
			}

			targetId = Long.parseLong(target);
			roleId = Long.parseLong(targetRole);
		} catch(Exception ignored) {
			hook.editOriginal("Invalid args").queue();
			return;
		}

		Role role = Main.guild.getRoleById(roleId);
		if(role == null) {
			hook.editOriginal("No role found").queue();
			return;
		}

		Main.guild.retrieveMemberById(targetId).queue(member -> {
			boolean hadRole = Utils.hasRole(member, role);
			var request = !hadRole
					? Main.guild.addRoleToMember(member, role)
					: Main.guild.removeRoleFromMember(member, role);

			request.queue(
					i_ -> hook.editOriginal("Done (" + (!hadRole ? "GAVE" : "REMOVED") + ")").queue(),
					err -> hook.editOriginal("Failed: " + err).queue());
		}, err -> hook.editOriginal("No person found").queue());
	}

	private static void handleOpcode3_4(InteractionHook hook, List<Invite> list) {
		list = list.stream()
				.filter(invite -> invite.getInviter() != null && invite.getUses() > 0)
				.sorted(Comparator.comparingInt(Invite::getUses).reversed())
				.toList();
		if(list.isEmpty()) {
			hook.editOriginal("None to display.").queue();
			return;
		}

		StringBuilder builder = new StringBuilder();
		AtomicInteger r = new AtomicInteger(), g = new AtomicInteger(), b = new AtomicInteger();
		AtomicInteger factors = new AtomicInteger();
		RestAction<?> actions = null;

		for(Invite invite : list) {
			int uses = invite.getUses();

			builder.append('\n');
			// noinspection DataFlowIssue - checked in stream filter
			builder.append(invite.getInviter().getAsMention());
			builder.append(" made an invite with ")
					.append(uses).append(" use").append(uses == 1 ? "" : "s");

			RestAction<Member> action = Main.guild.retrieveMember(invite.getInviter());
			action = action.onSuccess(member -> {
				if(member == null) return;
				TeamRolePair pair = TeamRoleUtils.getTeamRoles(member);
				if(pair.baseRole() == null) return;
				Color color = pair.baseRole().getColor();
				if(color == null) return;

				r.addAndGet(color.getRed() * uses);
				g.addAndGet(color.getBlue() * uses);
				b.addAndGet(color.getBlue() * uses);
				factors.addAndGet(uses);
			});

			actions = (actions == null) ? action : actions.and(action);
		}

		actions.queue(i_ -> {
			Color color;
			if(factors.get() > 0) {
				color = new Color(
						r.get() / factors.get(),
						g.get() / factors.get(),
						b.get() / factors.get()
				);

				builder.append("\nWeighted Team Color *(weight based on invite uses)*: ")
						.append(Utils.colorToString(color));
			} else {
				color = Utils.randomColor();
			}

			if(builder.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
				hook.editOriginal("Too long to display!").queue();
			} else {
				hook.editOriginalEmbeds(new EmbedBuilder()
						.setTitle("Invites:")
						.setDescription(builder.toString())
						.setColor(color)
						.build()).queue();
			}
		});
	}
}
