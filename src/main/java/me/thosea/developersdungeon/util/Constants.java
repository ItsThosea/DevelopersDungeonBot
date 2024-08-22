package me.thosea.developersdungeon.util;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.thosea.developersdungeon.Main;

public final class Constants {
	public static final String VERSION = str("version");
	public static final String TOKEN = str("token");

	public static final long SERVER_ID = id("server_id");
	public static final LongSet ADMINS = idSet("admins");

	public static class Channels {
		public static final long GENERAL = id("channels.general");
		public static final long INFORMATION = id("channels.information");

		public static final long VERIFY = id("channels.verify");
		public static final long SUGGESTIONS = id("channels.suggestions");

		public static final long ANNOUNCEMENTS = id("channels.announcements");

		public static final long MINOR_LOG = id("channels.minor_log");
		public static final long MAJOR_LOG = id("channels.major_log");
		public static final long CHANNEL_LOG = id("channels.channel_log");

		public static final long BOTS = id("channels.bots");

		public static final long COMMISSIONS = id("channels.commissions");

		public static final long APPLICATIONS = id("channels.applications");

		public static final long COUNTS = id("channels.counts_channel");
		public static final long VOTING = id("counts.voting_channel");
	}

	public static class Categories {
		public static final long PRIVATE_CHANNELS = id("categories.private_channels");
		public static final long ARCHIVED_CHANNELS = id("categories.archived_channels");
	}

	public static class MessageContent {
		public static final String CF_PING = str("content.curseforge_ping");
	}

	public static class Roles {
		public static final long TEAM_ROLE_SANDWICH_TOP = id("roles.team_role_sandwich_top");
		public static final long TEAM_ROLE_SANDWICH_BOTTOM = id("roles.team_role_sandwich_bottom");

		public static final long STAFF = id("roles.staff");
		public static final long VERIFIED = id("roles.verified");
		public static final long CONTENT_CREATOR = id("roles.content_creator");
	}

	public static String str(String key) {
		String result = Main.properties.getProperty(key);
		if(result == null) throw new IllegalStateException("No property " + key);
		return result;
	}
	public static long id(String key) {
		try {
			long result = Long.parseLong(str(key));
			if(result < -10) throw new IllegalStateException("Negative values cannot be below -10");
			return result;
		} catch(NumberFormatException | IllegalStateException e) {
			throw new IllegalStateException("Invalid ID for " + key, e);
		}
	}
	@SuppressWarnings("SameParameterValue")
	public static LongSet idSet(String key) {
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