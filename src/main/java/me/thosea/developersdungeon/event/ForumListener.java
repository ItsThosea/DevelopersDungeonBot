package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.event.button.ButtonHandler;
import me.thosea.developersdungeon.util.ForumUtils;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;

public class ForumListener extends ListenerAdapter {
	private static final Button EDIT_STATUS_BUTTON = Button.success(ButtonHandler.ID_EDIT_STATUS, "Change Status");

	@Override
	public void onChannelCreate(ChannelCreateEvent event) {
		if(!(event.getChannel() instanceof ThreadChannel channel)) return;

		long parentId = channel.getParentChannel().getIdLong();
		if(parentId != 1237699021804671039L) return;

		Main.guild.retrieveMemberById(channel.getOwnerIdLong()).queue(member -> {
			Utils.logChannel("%s made commission request %s > %s", member, channel.getName(), channel);
			channel.sendMessageEmbeds(ForumUtils.makeStatusEmbed("Looking for somebody!"), ForumUtils.makeChannelsEmbed(null))
					.setAllowedMentions(List.of())
					.setMessageReference(channel.getIdLong())
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
