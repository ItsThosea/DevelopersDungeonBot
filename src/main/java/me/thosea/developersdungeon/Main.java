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

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
	private static final String TOKEN = "***REMOVED***";

	public static JDA jda;
	public static Guild guild;

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
		if(guild.getIdLong() != 989441509193551874L) {
			guild.leave().queue();
			return;
		}

		if(Main.guild != null) return;

		System.out.println("Found Developers Dungeon server, initializing");
		Main.guild = guild;

		if((minorLogChannel = guild.getTextChannelById(1254944964891115520L)) == null) {
			oops("No minor log channel found.");
		}
		if((majorLogChannel = guild.getTextChannelById(1237689893971562498L)) == null) {
			oops("No major log channel found.");
		}
		if((channelLogChannel = guild.getTextChannelById(1256002915647098890L)) == null) {
			oops("No channel log channel found.");
		}

		if((teamRoleSandwichTop = guild.getRoleById(1256007545152077884L)) == null) {
			oops("No team role sandwich top");
		}
		if((teamRoleSandwichBottom = guild.getRoleById(1256007657144188968L)) == null) {
			oops("No team role sandwich bottom");
		}
	}

	private static void oops(String error) {
		System.err.println("Error: " + error);
		jda.shutdownNow();
		System.exit(1);
	}

}