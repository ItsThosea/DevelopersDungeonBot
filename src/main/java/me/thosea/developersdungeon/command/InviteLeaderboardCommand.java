package me.thosea.developersdungeon.command;

import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.thosea.developersdungeon.Main;
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
import net.dv8tion.jda.api.requests.RestAction;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

		event.deferReply().queue(hook -> Main.guild.retrieveInvites().queue(invites -> {
			makeResponse(hook, invites);
		}));
	}

	public static void makeResponse(InteractionHook hook, List<Invite> list) {
		Long2IntOpenHashMap map = new Long2IntOpenHashMap();
		LongSet multipleInvites = new LongOpenHashSet();

		AtomicInteger r = new AtomicInteger(), g = new AtomicInteger(), b = new AtomicInteger();
		AtomicInteger factors = new AtomicInteger();

		RestAction<?> actions = null;

		for(Invite invite : list) {
			if(invite.getInviter() == null) continue;
			int uses = invite.getUses();
			if(uses < 1) continue;

			map.compute(invite.getInviter().getIdLong(), (_, num) -> {
				if(num == null) {
					return uses;
				} else {
					multipleInvites.add(invite.getInviter().getIdLong());
					return num + uses;
				}
			});

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

		if(actions == null) {
			hook.editOriginal("None to display.").queue();
			return;
		}

		actions.queue(_ -> {
			StringBuilder builder = new StringBuilder();

			map.long2IntEntrySet()
					.stream()
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