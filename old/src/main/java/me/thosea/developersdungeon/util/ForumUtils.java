package me.thosea.developersdungeon.util;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.button.ButtonHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

public final class ForumUtils {
	private ForumUtils() {}

	public static void sendBotMessage(ThreadChannel channel) {
		channel.sendMessageEmbeds(
						ForumUtils.makeStatusEmbed("Looking for somebody!"),
						ForumUtils.makeChannelsEmbed(null))
				.setAllowedMentions(List.of())
				.setMessageReference(channel.getIdLong())
				.setActionRow(
						Button.primary(
								ButtonHandler.ID_MAKE_CHANNEL + "-" + channel.getOwnerId(),
								"Create Private Channel"),
						Button.success(ButtonHandler.ID_EDIT_STATUS, "Change Status"))
				.queue();
	}

	public static void getBotMessage(MessageChannel channel, Consumer<Message> handler) {
		if(!isCommissionRequest(channel)) {
			handler.accept(null);
			return;
		}

		channel.getHistoryFromBeginning(5).queue(history -> {
			List<Message> list = history.getRetrievedHistory();

			for(int i = list.size() - 1; i >= 0; i--) {
				Message msg = list.get(i);
				if(!msg.getAuthor().equals(Main.jda.getSelfUser())) continue;
				if(msg.getEmbeds().isEmpty()) continue;

				handler.accept(msg);
				return;
			}

			handler.accept(null);
		});
	}

	public static boolean isCommissionRequest(Channel channel) {
		return channel instanceof ThreadChannel thread && thread.getParentChannel().getIdLong() == Constants.Channels.COMMISSIONS;
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