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
		public static final long MINOR_LOG_CHANNEL = id("channels.minor_log");
		public static final long MAJOR_LOG_CHANNEL = id("channels.major_log");
		public static final long CHANNEL_LOG_CHANNEL = id("channels.channel_log");

		public static final long BOTS_CHANNEL = id("channels.bots");

		public static final long COMMISSIONS_CHANNEL = id("channels.commissions");
	}

	public static class Categories {
		public static final long PRIVATE_CHANNEL_CATEGORY = id("categories.private_channels");
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
			return Long.parseLong(str(key));
		} catch(NumberFormatException e) {
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
