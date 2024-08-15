package me.thosea.developersdungeon.util;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.Main.StreamHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.internal.entities.MemberImpl;

import java.awt.Color;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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

	public static long getNextCurseforgePingTime() {
		var date = ZonedDateTime.now();
		var target = LocalDate.now()
				.atTime(19, 0)
				.atZone(ZoneOffset.UTC)
				.plusMonths(1)
				.withDayOfMonth(1)
				.minusDays(2);

		if(date.compareTo(target) >= 0) {
			date = target.plusMonths(2)
					.withDayOfMonth(1)
					.minusDays(2);
		} else {
			date = target;
		}

		return date.toInstant().toEpochMilli();
	}

	public static void logMinor(String message, Object... args) {
		logMinor(message.formatted(transformArgs(args)));
	}

	public static void logMinor(String message) {
		System.out.println("[MINOR] " + message);
		if(Main.minorLogChannel != null) {
			Main.minorLogChannel.sendMessage(message)
					.setAllowedMentions(List.of())
					.setSuppressEmbeds(true)
					.queue();
		}
	}

	public static void logMajor(String message, Object... args) {
		logMajor(message.formatted(transformArgs(args)));
	}

	public static void logMajor(String message) {
		System.out.println("[MAJOR] " + message);
		if(Main.majorLogChannel != null) {
			Main.majorLogChannel.sendMessage(message)
					.setAllowedMentions(List.of())
					.setSuppressEmbeds(true)
					.queue();
		}
	}

	public static void logChannel(String message, Object... args) {
		logChannel(message.formatted(transformArgs(args)));
	}

	public static void logChannel(String message) {
		System.out.println("[CHANNEL] " + message);
		if(Main.channelLogChannel != null) {
			Main.channelLogChannel.sendMessage(message)
					.setAllowedMentions(List.of())
					.setSuppressEmbeds(true)
					.queue();
		}
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
				} else if(args[i] instanceof Role role) {
					str += " (@" + role.getName() + ")";
				}

				args[i] = str;
			}
		}

		return args;
	}

	public static Color randomColor() {
		var random = ThreadLocalRandom.current();
		return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
	}

	public static Color parseColor(String colorStr, SlashCommandInteraction event) {
		colorStr = colorStr.replaceAll("\\s", "");
		if(colorStr.equalsIgnoreCase("random")) {
			return randomColor();
		}

		try {
			return Color.decode(colorStr);
		} catch(NumberFormatException e) {
			// not valid hex, try RGB
		}

		try {
			String[] rgbValues = colorStr.split(",");
			if(rgbValues.length > 3) {
				throw new Exception();
			}
			int r = Integer.parseInt(rgbValues[0]);
			int g = Integer.parseInt(rgbValues[1]);
			int b = Integer.parseInt(rgbValues[2]);
			return new Color(r, g, b);
		} catch(Exception e) {
			event.reply("Invalid color. Must be a hex code or R,G,B (spaces are ignored, e.g. 255, 64, 100)\n" +
							"*(Protip: use https://g.co/kgs/gGWjRYR or type \"random\" for a random color)*")
					.setEphemeral(true)
					.queue();
			return null;
		}
	}

	public static boolean hasRole(Member member, Role role) {
		return ((MemberImpl) member).getRoleSet().contains(role);
	}

	public static boolean hasRole(Member member, long roleId) {
		Role role = Main.guild.getRoleById(roleId);
		return role != null && hasRole(member, role);
	}

	public static boolean isValidUrl(String url) {
		return url.length() <= MessageEmbed.URL_MAX_LENGTH && EmbedBuilder.URL_PATTERN.matcher(url).matches();
	}

	public static String colorToString(Color color) {
		String hex = "#" + Integer.toHexString(color.getRGB()).substring(2);
		return hex + " (" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
	}

	public static void loadResource(String name, StreamHandler handler) {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();

		try(InputStream stream = loader.getResourceAsStream(name)) {
			handler.accept(stream);
		} catch(Exception e) {
			throw new IllegalStateException("Failed to read file from " + name, e);
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