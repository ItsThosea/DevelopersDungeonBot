package me.thosea.developersdungeon;

import me.thosea.developersdungeon.command.CommandHandler;
import me.thosea.developersdungeon.event.AutoReactionListener;
import me.thosea.developersdungeon.event.ButtonListener;
import me.thosea.developersdungeon.event.EchoMessageListener;
import me.thosea.developersdungeon.event.ForumListener;
import me.thosea.developersdungeon.event.LeaveListener;
import me.thosea.developersdungeon.event.LogMessageListener;
import me.thosea.developersdungeon.event.ModalResponseListener;
import me.thosea.developersdungeon.event.PChannelListener;
import me.thosea.developersdungeon.event.PingResponseMessageListener;
import me.thosea.developersdungeon.event.SlashCommandListener;
import me.thosea.developersdungeon.util.Constants;
import me.thosea.developersdungeon.util.Utils;
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
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Properties;
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
				new PingResponseMessageListener());
		jda.updateCommands().addCommands(CommandHandler.buildCommands()).queue();
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

		if((generalChannel = guild.getTextChannelById(Constants.Channels.GENERAL_CHANNEL)) == null) {
			System.out.println("No general channel found. Won't send welcome messages.");
		}
		if((minorLogChannel = guild.getTextChannelById(Constants.Channels.MINOR_LOG_CHANNEL)) == null) {
			System.out.println("No minor log channel found. Won't send minor logs to discord.");
		}
		if((majorLogChannel = guild.getTextChannelById(Constants.Channels.MAJOR_LOG_CHANNEL)) == null) {
			System.out.println("No major log channel found. Won't send major logs to discord.");
		}
		if((channelLogChannel = guild.getTextChannelById(Constants.Channels.CHANNEL_LOG_CHANNEL)) == null) {
			System.out.println("No channel log channel found. Won't send channel logs to discord.");
		}

		if((teamRoleSandwichTop = guild.getRoleById(Constants.Roles.TEAM_ROLE_SANDWICH_TOP)) == null) {
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