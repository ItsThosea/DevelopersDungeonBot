package me.thosea.developersdungeon.util;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.thosea.developersdungeon.Main;

public final class Constants {
	public static final String VERSION = str("version");
	public static final String TOKEN = str("token");

	public static final long SERVER_ID = id("server_id");
	public static final LongSet ADMINS = idSet("admins");

	public static class Channels {
		public static final long GENERAL_CHANNEL = id("channels.general");
		public static final long INFORMATION_CHANNEL = id("channels.information");

		public static final long VERIFY_CHANNEL = id("channels.verify");
		public static final long SUGGESTIONS_CHANNEL = id("channels.suggestions");

		public static final long ANNOUNCEMENTS_CHANNEL = id("channels.announcements");

		public static final long MINOR_LOG_CHANNEL = id("channels.minor_log");
		public static final long MAJOR_LOG_CHANNEL = id("channels.major_log");
		public static final long CHANNEL_LOG_CHANNEL = id("channels.channel_log");

		public static final long BOTS_CHANNEL = id("channels.bots");

		public static final long COMMISSIONS_CHANNEL = id("channels.commissions");
	}

	public static class Categories {
		public static final long PRIVATE_CHANNEL_CATEGORY = id("categories.private_channels");
		public static final long ARCHIVED_CHANNELS_CATEGORY = id("categories.archived_channels");
	}

	public static class Messages {
		public static final long SUGGESTION_COUNT = id("messages.suggestion_count");
	}

	public static class MessageContent {
		public static final String CF_PING = str("content.curseforge_ping");
	}

	public static class Roles {
		public static final long TEAM_ROLE_SANDWICH_TOP = id("roles.team_role_sandwich_top");
		public static final long TEAM_ROLE_SANDWICH_BOTTOM = id("roles.team_role_sandwich_bottom");

		public static final long STAFF = id("roles.staff");
		public static final long VERIFIED = id("roles.verified");
	}

	private static String str(String key) {
		String result = Main.properties.getProperty(key);
		if(result == null) throw new IllegalStateException("No property " + key);
		return result;
	}
	private static long id(String key) {
		try {
			long result = Long.parseLong(str(key));
			if(result < -10) throw new IllegalStateException("Negative values cannot be below -10");
			return result;
		} catch(NumberFormatException | IllegalStateException e) {
			throw new IllegalStateException("Invalid ID for " + key, e);
		}
	}
	@SuppressWarnings("SameParameterValue")
	private static LongSet idSet(String key) {
		String value = str(key);
		String[] split = value.split(" ");
		long[] ids = new long[split.length];
		for(int i = 0; i < split.length; i++) {
			try {
				ids[i] = Long.parseLong(split[i]);
			} catch(NumberFormatException e) {
				throw new IllegalStateException("Invalid ID list for " + key, e);
			}
		}
		return LongSet.of(ids);
	}
}
