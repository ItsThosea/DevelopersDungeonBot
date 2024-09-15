package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class CommissionChannelsCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("commissionchannels", "Manipulate channels displayed in the bot's status message at the beginning of a commission request.")
				.addSubcommands(new SubcommandData("clear", "Clear all displayed channels"))
				.addSubcommands(new SubcommandData("add", "Add the specified channel(s)")
						.addOption(OptionType.STRING, "channel", "Channel", true))
				.addSubcommands(new SubcommandData("remove", "Remove the specified channel(s)")
						.addOption(OptionType.STRING, "channel", "Channel", true))
				.addSubcommands(new SubcommandData("list", "List all channels already displayed"));
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		if(!ForumUtils.isCommissionRequest(event.getChannel())) {
			event.reply("Not a commission request thread.")
					.setEphemeral(true)
					.queue();
			return;
		}

		ThreadChannel thread = event.getChannel().asThreadChannel();
		String command = event.getSubcommandName();
		assert command != null;

		if(!command.equals("list") && thread.getOwnerIdLong() != member.getIdLong() && !Utils.isAdmin(member)) {
			event.reply("You don't have permission to do that.")
					.setEphemeral(true)
					.queue();
			return;
		}

		event.deferReply().setEphemeral(true).queue(hook -> ForumUtils.getBotMessage(thread, message -> {
			if(message == null) {
				hook.editOriginal("Could not find the bot message.").queue();
				return;
			}

			MessageEmbed embed = message.getEmbeds().get(1);
			MessageEmbed newEmbed;

			String suffix = "\nMessage: " + message.getJumpUrl();
			String prev = ForumUtils.getPreviousChannels(message);

			switch(command) {
				case "list" -> {
					hook.editOriginalEmbeds(embed).setContent(suffix).queue();
					return;
				}
				case "clear" -> newEmbed = handleClear(hook, message, prev, suffix);
				case "add" -> newEmbed = handleAdd(event, message, hook, prev, suffix);
				case "remove" -> newEmbed = handleRemove(event, message, hook, prev, suffix);
				default -> {
					hook.editOriginal("Invalid subcommand, this is a bug.").queue();
					return;
				}
			}

			if(newEmbed == null) return;

			message.editMessageEmbeds(message.getEmbeds().getFirst(), newEmbed).queue();
		}));
	}

	private MessageEmbed handleClear(InteractionHook hook, Message message,
	                                 String prev, String suffix) {
		if(prev == null) {
			hook.editOriginal("There are no channels." + suffix).queue();
		} else {
			hook.editOriginal("Cleared listed channels." + suffix).queue();
		}

		Utils.logMinor("%s cleared all listed channels in commission request %s > %s",
				hook.getInteraction().getMember(),
				hook.getInteraction().getChannel(),
				message);
		return ForumUtils.makeChannelsEmbed(null);
	}

	private MessageEmbed handleAdd(SlashCommandInteraction event,
	                               Message message, InteractionHook hook,
	                               String prev, String suffix) {
		String arg = this.getChannels(event);

		if(arg == null) {
			hook.editOriginal("Invalid channel(s).").queue();
			return null;
		}

		StringBuilder builder = new StringBuilder(arg.length() + (prev == null ? 0 : prev.length()));
		if(prev != null) builder.append(prev);

		boolean isFirst = true;
		for(String channel : arg.split(" ")) {
			if(channel.isBlank()) continue;

			if(builder.indexOf(channel) != -1) {
				hook.editOriginal("Duplicate channel: " + channel + suffix).queue();
				return null;
			} else {
				if(isFirst) {
					isFirst = false;
				} else {
					builder.append(", ");
				}
				builder.append(channel);
			}
		}

		hook.editOriginal("Added " +
				(arg.contains(" ") ? "channels " : "channel ")
				+ arg + suffix).queue();
		Utils.logMinor("%s added channel(s) %s from commission request %s > %s",
				event.getMember(),
				arg,
				event.getChannel(),
				message);
		return ForumUtils.makeChannelsEmbed(builder.toString());
	}

	private MessageEmbed handleRemove(SlashCommandInteraction event,
	                                  Message message, InteractionHook hook,
	                                  String prev, String suffix) {
		String channels = this.getChannels(event);

		if(channels == null) {
			hook.editOriginal("Invalid channel(s).").queue();
			return null;
		} else if(prev == null) {
			hook.editOriginal("There are no channels." + suffix).queue();
			return null;
		}

		String[] split = channels.split(" ");
		for(String channel : split) {
			String replace = prev.replaceAll("(\\s,\\s)?" + channel + "(\\s)?", "");
			if(replace.equals(prev)) {
				hook.editOriginal("Channel not present: " + channel + suffix).queue();
				return null;
			} else {
				prev = replace;
			}
		}

		hook.editOriginal("Removed the specified "
						+ (split.length == 1 ? "channel" : "channels")
						+ suffix)
				.queue();
		Utils.logMinor("%s removed channel(s) %s from commission request %s > %s",
				event.getMember(),
				channels,
				event.getChannel(),
				message);
		return ForumUtils.makeChannelsEmbed(!prev.equals("<#") ? null : prev);
	}

	private String getChannels(SlashCommandInteraction event) {
		return Utils.splitChannelMentions(event.getOption("channel", OptionMapping::getAsString));
	}

}