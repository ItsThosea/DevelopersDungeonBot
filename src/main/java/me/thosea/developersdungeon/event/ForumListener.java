package me.thosea.developersdungeon.event;

import it.unimi.dsi.fastutil.longs.LongOpenHashBigSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.event.button.ButtonHandler;
import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ForumListener extends ListenerAdapter {
	private static final Button EDIT_STATUS_BUTTON = Button.success(ButtonHandler.ID_EDIT_STATUS, "Change Status");
	private static final LongSet WAITING_FOR_MESSAGE = new LongOpenHashBigSet();

	@Override
	public void onChannelCreate(ChannelCreateEvent event) {
		if(!(event.getChannel() instanceof ThreadChannel channel)) return;

		long parentId = channel.getParentChannel().getIdLong();
		if(parentId != 1237699021804671039L) return;

		WAITING_FOR_MESSAGE.add(channel.getIdLong());
		Utils.doLater(TimeUnit.SECONDS, 30, () -> {
			if(WAITING_FOR_MESSAGE.contains(channel.getIdLong())) {
				Utils.logChannel("Warning: Thread %s didn't have its first message sent", channel);
			}
		});
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if(!WAITING_FOR_MESSAGE.remove(event.getChannel().getIdLong())) return;

		ThreadChannel channel = event.getChannel().asThreadChannel();

		Main.guild.retrieveMemberById(channel.getOwnerIdLong()).queue(member -> {
			Utils.logChannel("%s made commission request %s > %s", member, channel.getName(), channel);
			channel.sendMessageEmbeds(ForumUtils.makeStatusEmbed("Looking for somebody!"), ForumUtils.makeChannelsEmbed(null))
					.setAllowedMentions(List.of())
					.setMessageReference(event.getMessage().getIdLong())
					.setActionRow(makeMakeChannelButton(channel), EDIT_STATUS_BUTTON)
					.queue();
		});
	}

	private static Button makeMakeChannelButton(ThreadChannel channel) {
		return Button.primary(
				ButtonHandler.ID_MAKE_CHANNEL + "-" + channel.getOwnerId(),
				"Create Private Channel");
	}
}
