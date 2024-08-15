package me.thosea.developersdungeon.util;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.button.ButtonHandler;
import me.thosea.developersdungeon.util.Constants.Categories;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class PChannelUtils {
	private PChannelUtils() {}

	public static MessageEmbed makeEmbed(String user, @Nullable String channelLink) {
		return new EmbedBuilder()
				.appendDescription("The legendary beginning of " + user + "'s private channel.")
				.appendDescription(channelLink == null ? "" : "\nCommission: " + channelLink)
				.setColor(new Color(252, 198, 3))
				.build();
	}

	public static boolean isPrivateChannel(MessageChannel channel) {
		if(channel instanceof ICategorizableChannel categorizable) {
			long id = categorizable.getParentCategoryIdLong();
			return id == Categories.PRIVATE_CHANNELS || id == Categories.ARCHIVED_CHANNELS;
		} else {
			return false;
		}
	}

	public static boolean isArchived(MessageChannel channel) {
		if(channel instanceof ICategorizableChannel categorizable) {
			return categorizable.getParentCategoryIdLong() == Categories.ARCHIVED_CHANNELS;
		} else {
			return false;
		}
	}

	public static void getMessageAndOwner(MessageChannel channel, BiConsumer<Message, String> handler) {
		channel.getHistoryFromBeginning(3).queue(history -> {
			for(Message message : history.getRetrievedHistory()) {
				if(!message.getAuthor().equals(Main.jda.getSelfUser())) continue;
				if(message.getActionRows().isEmpty()) continue;

				ActionRow row = message.getActionRows().getFirst();
				if(row.getButtons().isEmpty()) continue;

				Button button = row.getButtons().getFirst();
				if(button.getId() == null) continue;
				if(!button.getId().startsWith(ButtonHandler.ID_PCHANNEL_HELP)) continue;

				handler.accept(message, button.getId().substring(ButtonHandler.ID_PCHANNEL_HELP.length() + 1));
				return;
			}

			handler.accept(null, null);
		});
	}

	public static void getOwner(MessageChannel channel, Consumer<String> handler) {
		getMessageAndOwner(channel, (_, owner) -> handler.accept(owner));
	}

	public static void getBotMessage(MessageChannel channel, Consumer<Message> handler) {
		getMessageAndOwner(channel, (msg, _) -> handler.accept(msg));
	}

}