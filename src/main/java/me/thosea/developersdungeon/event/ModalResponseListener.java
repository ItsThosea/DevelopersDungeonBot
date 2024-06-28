package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.command.MakePrivateChannelCommand;
import me.thosea.developersdungeon.command.SetCommissionStateCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ModalResponseListener extends ListenerAdapter {
	public static final String MODAL_EDIT_STATUS = "devdungeon_editstatus";
	public static final String MODAL_MAKE_CHANNEL = "devdungeon_makechannel";

	@Override
	public void onModalInteraction(ModalInteractionEvent event) {
		if(event.getMember() == null) return;

		Member member = event.getMember();
		if(event.getModalId().equals(MODAL_EDIT_STATUS)) {
			String status = event.getValue("status").getAsString();

			event.deferReply().setEphemeral(true).queue(hook -> {
				SetCommissionStateCommand.handle(
						member,
						event.getChannel().asThreadChannel(),
						status,
						msg -> hook.editOriginal(msg).queue());
			});
		} else if(event.getModalId().equals(MODAL_MAKE_CHANNEL)) {
			String name = event.getValue("channel_name").getAsString();

			event.deferReply().setEphemeral(true).queue(hook -> {
				MakePrivateChannelCommand.handle(member, name, "text", null, hook);
			});
		}
	}
}
