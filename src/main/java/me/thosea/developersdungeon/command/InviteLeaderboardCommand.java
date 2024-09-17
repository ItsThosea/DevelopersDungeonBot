package me.thosea.developersdungeon.command;

import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.AverageColorCounter;
import me.thosea.developersdungeon.util.Constants;
import me.thosea.developersdungeon.util.TeamRoleUtils;
import me.thosea.developersdungeon.util.TeamRoleUtils.TeamRolePair;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

public class InviteLeaderboardCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("inviteleaderboard", "Who's invited the most people?");
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

		event.deferReply().queue(hook -> {
			Thread.ofVirtual().start(() -> {
				respond(hook, Main.guild.retrieveInvites().complete());
			});
		});
	}

	private void respond(InteractionHook hook, List<Invite> list) {
		Long2IntOpenHashMap inviteCounts = new Long2IntOpenHashMap();
		LongSet multipleInvites = new LongOpenHashSet();
		AverageColorCounter colorCounter = new AverageColorCounter();

		for(Invite invite : list) {
			if(invite.getInviter() == null) continue;
			int uses = invite.getUses();
			if(uses < 1) continue;

			inviteCounts.compute(invite.getInviter().getIdLong(), (_, num) -> {
				if(num == null) {
					return uses;
				} else {
					multipleInvites.add(invite.getInviter().getIdLong());
					return num + uses;
				}
			});

			Member member = Utils.getSafe(Main.guild.retrieveMember(invite.getInviter())::complete);
			TeamRolePair pair = member == null ? null : TeamRoleUtils.getTeamRoles(member);
			if(pair != null && pair.baseRole() != null)
				colorCounter.addColor(pair.baseRole().getColor());
		}

		if(inviteCounts.isEmpty()) {
			hook.editOriginal("None to display.").queue();
			return;
		}

		StringBuilder builder = new StringBuilder();

		inviteCounts.long2IntEntrySet().stream()
				.sorted(Comparator.comparingInt(Entry::getIntValue).reversed())
				.forEach(entry -> {
					builder.append('\n');
					builder.append("<@").append(entry.getLongKey()).append(">");

					builder.append(" made ");
					if(multipleInvites.contains(entry.getLongKey())) {
						builder.append(" invites ");
					} else {
						builder.append(" an invite ");
					}
					builder.append(" with ");

					int uses = entry.getIntValue();
					builder.append(uses).append(" use").append(uses == 1 ? "" : "s");
				});

		Color color;
		if(colorCounter.factors() > 0) {
			color = colorCounter.average();

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
	}
}