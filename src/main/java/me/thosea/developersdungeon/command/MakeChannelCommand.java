package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.event.button.ButtonHandler;
import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.PChannelUtils;
import me.thosea.developersdungeon.util.TeamRoleUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class MakeChannelCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("makechannel", "Make a private channel that you have full access to.")
				.addSubcommands(new SubcommandData("text", "Make a text channel.")
						.addOption(OptionType.STRING, "name", "Name", true)
						.addOption(OptionType.STRING, "members", "Members With Access (can be changed later)"))
				.addSubcommands(new SubcommandData("voice", "Make a voice channel.")
						.addOption(OptionType.STRING, "name", "Name", true)
						.addOption(OptionType.STRING, "members", "Members/Roles With Access (can be changed later)"));
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		String members = event.getOption("members", OptionMapping::getAsString);
		String[] split;

		if(members != null) {
			members = Utils.splitUserAndRoleMentions(members);
			if(members == null) {
				event.reply("Invalid user/team role list.").setEphemeral(true).queue();
				return;
			}
			split = members.split(" ");
			if(split.length > 5) {
				event.reply("You can only add 5 users/team roles.").setEphemeral(true).queue();
				return;
			}
		} else {
			split = null;
		}

		event.deferReply().setEphemeral(true).queue(hook -> {
			handle(member,
					event.getOption("name", OptionMapping::getAsString),
					event.getSubcommandName(), split,
					hook);
		});
	}

	public static void handle(Member member, String channelName, String type,
	                          String[] split, InteractionHook hook) {
		channelName = member.getUser().getName() + "-" + channelName;
		if(channelName.length() > 100) {
			hook.editOriginal("That channel name is too long.").queue();
			return;
		}

		var request = type.equals("text")
				? Main.guild.createTextChannel(channelName, Main.guild.getCategoryById(1248544270579924993L))
				: Main.guild.createVoiceChannel(channelName, Main.guild.getCategoryById(1248544270579924993L));

		request.reason(member.getAsMention());

		final String finalChannelName = channelName;
		request.queue(channel -> {
			channel.sendMessageEmbeds(PChannelUtils.makeInitEmbed(member.getAsMention()))
					.setActionRow(Button.secondary(
							ButtonHandler.ID_PCHANNEL_HELP + "-" + member.getIdLong(),
							"Commands"
					))
					.queue(msg -> {
						handleNewChannel(member, hook, channel, msg, type, finalChannelName, split);
					});
		});
	}

	private static void handleNewChannel(Member member, InteractionHook hook,
	                                     StandardGuildChannel channel,
	                                     Message msg,
	                                     String type, String channelName,
	                                     String[] split) {
		if(channel instanceof TextChannel text) {
			text.pinMessageById(msg.getIdLong()).queue();
		}

		if(hook.getInteraction().getMessageChannel() instanceof ThreadChannel thread
				&& thread.getOwnerIdLong() == member.getIdLong()) {
			ForumUtils.getBotMessage(thread, stateMsg -> {
				if(stateMsg == null) return;

				String channels = ForumUtils.getPreviousChannels(stateMsg);
				if(channels == null) {
					channels = channel.getAsMention();
				} else {
					channels += ", " + channel.getAsMention();
				}

				stateMsg.editMessageEmbeds(
						stateMsg.getEmbeds().getFirst(),
						ForumUtils.makeChannelsEmbed(channels)).queue();
			});
		}

		if(split != null) {
			String errorMessage = """
					Made your %s channel > %s
					*Note: Some of the users/roles you specified weren't added because
					they either weren't valid members or weren't team roles.
					""".formatted(type, channel.getAsMention());

			for(String mention : split) {
				if(mention.startsWith("<@&")) {
					mention = mention.substring(3, mention.length() - 1);

					Role role = Main.guild.getRoleById(mention);
					if(role == null || !TeamRoleUtils.isTeamRole(role)) {
						hook.editOriginal(errorMessage).queue();
					} else {
						channel.upsertPermissionOverride(role)
								.grant(Permission.VIEW_CHANNEL)
								.queue();
					}
				} else {
					mention = mention.substring(2, mention.length() - 1);
					Main.guild.retrieveMemberById(mention).queue(toAdd -> {
						channel.upsertPermissionOverride(toAdd)
								.grant(Permission.VIEW_CHANNEL)
								.queue();
					}, err -> hook.editOriginal(errorMessage).queue());
				}
			}
		}

		channel.upsertPermissionOverride(member)
				.grant(Permission.VIEW_CHANNEL, Permission.MANAGE_PERMISSIONS, Permission.MANAGE_WEBHOOKS)
				.flatMap(i__ -> hook.editOriginal("Made your " + type + " channel > " + channel.getAsMention()))
				.queue();

		Utils.logChannel("%s made %s channel %s > %s", member, type, channelName, channel);
	}
}
