package me.thosea.developersdungeon;

import me.thosea.developersdungeon.command.CommandHandler;
import me.thosea.developersdungeon.event.ButtonListener;
import me.thosea.developersdungeon.event.EchoMessageListener;
import me.thosea.developersdungeon.event.ForumListener;
import me.thosea.developersdungeon.event.LeaveListener;
import me.thosea.developersdungeon.event.LogMessageListener;
import me.thosea.developersdungeon.event.ModalResponseListener;
import me.thosea.developersdungeon.event.PChannelListener;
import me.thosea.developersdungeon.event.SlashCommandListener;
import me.thosea.developersdungeon.util.Constants;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import okhttp3.OkHttpClient;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {
	private Main() {}

	private static final String TOKEN;
	public static final String VERSION;

	static {
		String filePath = "devdungeon.properties";

		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Properties properties = new Properties();

			try(InputStream stream = loader.getResourceAsStream(filePath)) {
				properties.load(stream);
			}

			VERSION = properties.getProperty("version");
			TOKEN = properties.getProperty("token");
		} catch(Exception e) {
			throw new IllegalStateException("Failed to read properties from " + filePath, e);
		}

		System.out.println("Running Developers Dungeon v" + VERSION);
	}

	public static JDA jda;
	public static Guild guild;

	public static TextChannel generalChannel;
	public static TextChannel minorLogChannel;
	public static TextChannel majorLogChannel;
	public static TextChannel channelLogChannel;

	public static Role teamRoleSandwichTop;
	public static Role teamRoleSandwichBottom;

	public static void main(String[] args) throws Exception {
		jda = JDABuilder.createDefault(TOKEN)
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
				new LeaveListener());
		jda.updateCommands().addCommands(CommandHandler.buildCommands()).queue();
	}

	private static void guildReady(GuildReadyEvent event) {
		Guild guild = event.getGuild();
		if(guild.getIdLong() != Constants.DEVELOPER_DUNGEONS_SERVER_ID) {
			guild.leave().queue();
			return;
		}

		if(Main.guild != null) return;

		System.out.println("Found Developers Dungeon server, initializing");
		Main.guild = guild;

		if((generalChannel = guild.getTextChannelById(Constants.Channels.GENERAL_CHANNEL)) == null) {
			oops("No general channel found.");
		}
		if((minorLogChannel = guild.getTextChannelById(Constants.Channels.MINOR_LOG_CHANNEL)) == null) {
			oops("No minor log channel found.");
		}
		if((majorLogChannel = guild.getTextChannelById(Constants.Channels.MAJOR_LOG_CHANNEL)) == null) {
			oops("No major log channel found.");
		}
		if((channelLogChannel = guild.getTextChannelById(Constants.Channels.CHANNEL_LOG_CHANNEL)) == null) {
			oops("No channel log channel found.");
		}

		if((teamRoleSandwichTop = guild.getRoleById(Constants.Roles.TEAM_ROLE_SANDWICH_BOTTOM)) == null) {
			oops("No team role sandwich top");
		}
		if((teamRoleSandwichBottom = guild.getRoleById(Constants.Roles.TEAM_ROLE_SANDWICH_BOTTOM)) == null) {
			oops("No team role sandwich bottom");
		}
	}

	private static void oops(String error) {
		System.err.println("Error: " + error);
		jda.shutdownNow();
		System.exit(1);
	}

}