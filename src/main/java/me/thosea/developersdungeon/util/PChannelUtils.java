package me.thosea.developersdungeon.util;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.event.button.ButtonHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.util.function.Consumer;

public final class PChannelUtils {
	private PChannelUtils() {}

	public static MessageEmbed makeInitEmbed(String user) {
		return new EmbedBuilder()
				.appendDescription("The legendary beginning of " + user + "'s private channel.")
				.setColor(new Color(252, 198, 3))
				.build();
	}

	public static boolean isPrivateChannel(MessageChannel channel) {
		return channel instanceof ICategorizableChannel categorizable //?????
				&& categorizable.getParentCategoryIdLong() == 1248544270579924993L;
	}

	public static void getOwner(MessageChannel channel, Consumer<String> handler) {
		channel.getHistoryFromBeginning(3).queue(history -> {
			for(Message message : history.getRetrievedHistory()) {
				if(!message.getAuthor().equals(Main.jda.getSelfUser())) continue;
				if(message.getActionRows().isEmpty()) continue;

				ActionRow row = message.getActionRows().getFirst();
				if(row.getButtons().isEmpty()) continue;

				Button button = row.getButtons().getFirst();
				if(button.getId() == null) continue;
				if(!button.getId().startsWith(ButtonHandler.ID_PCHANNEL_HELP)) continue;

				handler.accept(button.getId().substring(ButtonHandler.ID_PCHANNEL_HELP.length() + 1));
				return;
			}

			handler.accept(null);
		});
	}

}
