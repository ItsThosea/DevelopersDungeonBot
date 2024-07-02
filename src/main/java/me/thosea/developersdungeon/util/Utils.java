package me.thosea.developersdungeon.util;

import me.thosea.developersdungeon.Main;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {
	private Utils() {}

	public static void doLater(TimeUnit unit, long time, Runnable action) {
		Main.jda.getRateLimitPool().schedule(action, time, unit);
	}

	public static boolean isBeingPinged(Message message) {
		Mentions mentions = message.getMentions();
		if(!mentions.isMentioned(Main.jda.getSelfUser(), MentionType.USER)) {
			return false;
		}

		if(message.getMessageReference() != null) { // replying to something
			return message.getContentRaw().contains(Main.jda.getSelfUser().getAsMention());
		}
		return true;
	}

	public static boolean isAdmin(Member member) {
		long id = member.getIdLong();
		return Constants.ADMINS.contains(id) || member.hasPermission(Permission.MANAGE_SERVER);
	}

	public static void logMinor(String message, Object... args) {
		logMinor(String.format(message, transformArgs(args)));
	}

	public static void logMinor(String message) {
		System.out.println("[MINOR] " + message);
		Main.minorLogChannel.sendMessage(message)
				.setAllowedMentions(List.of())
				.setSuppressEmbeds(true)
				.queue();
	}

	public static void logMajor(String message, Object... args) {
		logMajor(String.format(message, transformArgs(args)));
	}

	public static void logMajor(String message) {
		System.out.println("[MAJOR] " + message);
		Main.majorLogChannel.sendMessage(message)
				.setAllowedMentions(List.of())
				.setSuppressEmbeds(true)
				.queue();
	}

	public static void logChannel(String message, Object... args) {
		logChannel(String.format(message, transformArgs(args)));
	}

	public static void logChannel(String message) {
		System.out.println("[CHANNEL] " + message);
		Main.channelLogChannel.sendMessage(message)
				.setAllowedMentions(List.of())
				.setSuppressEmbeds(true)
				.queue();
	}

	private static Object[] transformArgs(Object[] args) {
		for(int i = 0; i < args.length; i++) {
			if(args[i] instanceof Message message) {
				args[i] = message.getJumpUrl();
			} else if(args[i] instanceof IMentionable mentionable) {
				String str = mentionable.getAsMention();

				if(args[i] instanceof Member member) {
					str += " (@" + member.getUser().getName() + ")";
				} else if(args[i] instanceof Channel channel) {
					str += " (#" + channel.getName() + ")";
				}

				args[i] = str;
			}
		}

		return args;
	}

	public static Color parseColor(String colorStr, SlashCommandInteraction event) {
		if(colorStr.equalsIgnoreCase("random")) {
			var random = ThreadLocalRandom.current();
			return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
		}

		try {
			String cleanedString = colorStr.replaceAll("\\s", "");
			String[] rgbValues = cleanedString.split(",");
			if(rgbValues.length > 3) {
				throw new Exception();
			}
			int r = Integer.parseInt(rgbValues[0]);
			int g = Integer.parseInt(rgbValues[1]);
			int b = Integer.parseInt(rgbValues[2]);
			return new Color(r, g, b);
		} catch(Exception e) {
			event.reply("Invalid color. Format: R,G,B (spaces are ignored, e.g. 255, 64, 100)\n" +
							"*(Protip: use https://g.co/kgs/gGWjRYR or type \"random\" for a random color)*")
					.setEphemeral(true)
					.queue();
			return null;
		}
	}

	public static String splitUserAndRoleMentions(String string) {
		return splitMentions(string, USER_PATTERN);
	}

	public static String splitChannelMentions(String string) {
		return splitMentions(string, CHANNEL_PATTERN);
	}

	private static final Pattern USER_PATTERN = Pattern.compile("<@(&)?(\\d+)>");
	private static final Pattern CHANNEL_PATTERN = Pattern.compile("<#(\\d+)>");

	private static String splitMentions(String string, Pattern pattern) {
		if(string == null) return null;

		StringBuilder builder = new StringBuilder();
		boolean isFirst = true;

		for(String part : string.split("[\\s>]+")) {
			if(part.isBlank()) continue;

			part += ">";
			Matcher matcher = pattern.matcher(part);
			if(!matcher.matches()) return null; // Invalid

			if(isFirst) {
				isFirst = false;
			} else {
				builder.append(' ');
			}
			builder.append(part);
		}

		return builder.isEmpty() ? null : builder.toString();
	}
}
