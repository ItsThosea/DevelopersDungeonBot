package me.thosea.developersdungeon.util;

import me.thosea.developersdungeon.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.awt.Color;
import java.util.function.Consumer;

public final class ForumUtils {
	private ForumUtils() {}

	public static void getBotMessage(MessageChannel channel, Consumer<Message> handler) {
		if(!isCommissionRequest(channel)) {
			handler.accept(null);
			return;
		}

		channel.getHistoryFromBeginning(5).queue(list -> {
			for(Message msg : list.getRetrievedHistory()) {
				if(msg.getAuthor().equals(Main.jda.getSelfUser())) {
					handler.accept(msg);
					return;
				}
			}

			handler.accept(null);
		});
	}

	public static boolean isCommissionRequest(Channel channel) {
		return channel instanceof ThreadChannel thread && thread.getParentChannel().getIdLong() == 1237699021804671039L;
	}

	public static MessageEmbed makeStatusEmbed(String status) {
		return new EmbedBuilder()
				.appendDescription("Status: " + status)
				.setColor(new Color(23, 230, 91))
				.build();
	}

	public static MessageEmbed makeChannelsEmbed(String channels) {
		if(channels == null) channels = "None";
		String prefix = channels.equals("None") || channels.contains(",")
				? "Channels: "
				: "Channel: ";

		return new EmbedBuilder()
				.appendDescription(prefix + channels)
				.setColor(new Color(22, 55, 162))
				.build();
	}

	public static String getPreviousChannels(Message message) {
		if(message.getEmbeds().size() < 2) return null;

		String desc = message.getEmbeds().get(1).getDescription();
		if(desc == null) return null;

		desc = desc.substring(desc.indexOf(':') + 2);
		return desc.equals("None") ? null : desc;
	}
}
