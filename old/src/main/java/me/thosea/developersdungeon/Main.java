package me.thosea.developersdungeon;

import me.thosea.developersdungeon.command.CommandHandler;
import me.thosea.developersdungeon.event.AutoReactionListener;
import me.thosea.developersdungeon.event.AutoThreadListener;
import me.thosea.developersdungeon.event.ButtonListener;
import me.thosea.developersdungeon.event.EchoMessageListener;
import me.thosea.developersdungeon.event.ForumListener;
import me.thosea.developersdungeon.event.LeaveListener;
import me.thosea.developersdungeon.event.LogMessageListener;
import me.thosea.developersdungeon.event.ModalResponseListener;
import me.thosea.developersdungeon.event.PChannelListener;
import me.thosea.developersdungeon.event.PingResponseMessageListener;
import me.thosea.developersdungeon.event.RoleRemoveEvent;
import me.thosea.developersdungeon.event.SlashCommandListener;
import me.thosea.developersdungeon.other.ChannelThreadCounter;
import me.thosea.developersdungeon.util.Constants;
import me.thosea.developersdungeon.util.Constants.Channels;
import me.thosea.developersdungeon.util.Constants.MessageContent;
import me.thosea.developersdungeon.util.Constants.Roles;
import me.thosea.developersdungeon.util.TeamRoleUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {
	public static Properties properties;

	@FunctionalInterface
	public interface StreamHandler {
		void accept(InputStream stream) throws Exception;
	}

	static {
		Utils.loadResource("devdungeon.properties", stream -> {
			properties = new Properties();
			properties.load(stream);
		});
		System.out.println("Running Developers Dungeon v" + Constants.VERSION);
	}

	public static JDA jda;
	public static Guild guild;

	@Nullable public static TextChannel generalChannel;
	@Nullable public static TextChannel minorLogChannel;
	@Nullable public static TextChannel majorLogChannel;
	@Nullable public static TextChannel channelLogChannel;
	@Nullable public static TextChannel countsChannel;
	@Nullable public static TextChannel votingChannel;

	public static Role teamRoleSandwichTop;
	public static Role teamRoleSandwichBottom;

	public static void main(String[] args) throws Exception {
		jda = JDABuilder.createDefault(Constants.TOKEN)
				.addEventListeners(new ListenerAdapter() {
					@Override
					public void onGuildReady(GuildReadyEvent event) {
						guildReady(event);
					}
				})
				.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
				.setStatus(OnlineStatus.ONLINE)
				.build();
		jda.awaitReady();
		// RestActionImpl.setDefaultFailure(error -> {}); // uncomment to hide exceptions
		Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);

		jda.addEventListener(
				new SlashCommandListener(),
				new EchoMessageListener(),
				new ForumListener(),
				new ButtonListener(),
				new ModalResponseListener(),
				new LogMessageListener(),
				new PChannelListener(),
				new LeaveListener(),
				new AutoReactionListener(),
				new PingResponseMessageListener(),
				new RoleRemoveEvent(),
				new AutoThreadListener());
		jda.updateCommands().addCommands(CommandHandler.buildCommands()).queue();

		scheduleCurseforgePing();

		// Fill up cache
		guild.getRoles().stream().filter(TeamRoleUtils::isTeamOwnerRole).forEach(TeamRoleUtils::getRoleOwner);
	}

	private static void scheduleCurseforgePing() {
		GuildMessageChannel channel = guild.getChannelById(GuildMessageChannel.class, Channels.ANNOUNCEMENTS);
		if(channel == null) {
			System.out.println("No valid announcements channel, won't schedule curseforge project ping.");
			return;
		}

		long time = Utils.getNextCurseforgePingTime();

		LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' HH:mm");
		Utils.logMinor("Next CF payout ping scheduled at %s", dateTime.format(formatter));

		Utils.doLater(TimeUnit.MILLISECONDS, time - System.currentTimeMillis(), () -> {
			String msg = """
					**We're close to the end of the month!** Don't forget to cash out the CurseForge rewards before the month ends.
					
					%s https://authors.curseforge.com/#/reward-store
					
					%s
					""".formatted(Emoji.fromUnicode("U+1F449"), MessageContent.CF_PING);

			channel.sendMessage(msg).queue();
			Utils.doLater(TimeUnit.MILLISECONDS, 2, Main::scheduleCurseforgePing);
		});
	}

	private static void guildReady(GuildReadyEvent event) {
		Guild guild = event.getGuild();
		if(guild.getIdLong() != Constants.SERVER_ID) {
			guild.leave().queue();
			return;
		}

		if(Main.guild != null) return;

		System.out.println("Found Developers Dungeon server, initializing");
		Main.guild = guild;

		if((generalChannel = guild.getTextChannelById(Channels.GENERAL)) == null) {
			System.out.println("No general channel found. Won't send welcome messages.");
		}
		if((minorLogChannel = guild.getTextChannelById(Channels.MINOR_LOG)) == null) {
			System.out.println("No minor log channel found. Won't send minor logs to discord.");
		}
		if((majorLogChannel = guild.getTextChannelById(Channels.MAJOR_LOG)) == null) {
			System.out.println("No major log channel found. Won't send major logs to discord.");
		}
		if((channelLogChannel = guild.getTextChannelById(Channels.CHANNEL_LOG)) == null) {
			System.out.println("No channel log channel found. Won't send channel logs to discord.");
		}
		if((countsChannel = guild.getTextChannelById(Channels.COUNTS)) == null) {
			System.out.println("No counts channel. Auto-threading will be disabled.");
		}
		if((votingChannel = guild.getTextChannelById(Channels.VOTING)) == null) {
			System.out.println("No voting channel. /smpsuggest will be disabled.");
		}

		if((teamRoleSandwichTop = guild.getRoleById(Roles.TEAM_ROLE_SANDWICH_TOP)) == null) {
			oops("No team role sandwich top");
		}
		if((teamRoleSandwichBottom = guild.getRoleById(Roles.TEAM_ROLE_SANDWICH_BOTTOM)) == null) {
			oops("No team role sandwich bottom");
		}

		new ChannelThreadCounter("suggestions", "Suggestion", true);
		new ChannelThreadCounter("modloader", "Modloader Announcement", false);
		new ChannelThreadCounter("platform", "Platform Announcement", false);
		new ChannelThreadCounter("minecraft", "Minecraft Announcement", false);
		new ChannelThreadCounter("voting", "SMP Suggestion", true);
		new ChannelThreadCounter("new_releases", "New Release", true);
		new ChannelThreadCounter("update_releases", "Update Release", true);
	}

	private static void oops(String error) {
		System.err.println("Error: " + error);
		jda.shutdownNow();
		System.exit(1);
	}

}