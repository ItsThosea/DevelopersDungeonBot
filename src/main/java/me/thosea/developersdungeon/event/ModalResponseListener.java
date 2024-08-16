package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.button.ButtonContentCreatorApp;
import me.thosea.developersdungeon.command.ApplyCommand;
import me.thosea.developersdungeon.command.MakeChannelCommand;
import me.thosea.developersdungeon.command.SetCommissionStatusCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

public class ModalResponseListener extends ListenerAdapter {
	public static final String MODAL_EDIT_STATUS = "devdungeon_editstatus";
	public static final String MODAL_MAKE_CHANNEL = "devdungeon_makechannel";

	public static final String MODAL_APPLICATION = "devdungeon_application";
	public static final String MODAL_APPLICATION_DENY = "devdungeon_application_deny";

	@Override
	public void onModalInteraction(ModalInteractionEvent event) {
		if(event.getMember() == null) return;

		Member member = event.getMember();
		String id = event.getModalId();

		if(id.equals(MODAL_EDIT_STATUS)) {
			String status = event.getValue("status").getAsString();

			event.deferReply().setEphemeral(true).queue(hook -> {
				SetCommissionStatusCommand.handle(
						member,
						event.getChannel().asThreadChannel(),
						status,
						msg -> hook.editOriginal(msg).queue());
			});
		} else if(id.equals(MODAL_MAKE_CHANNEL)) {
			String name = event.getValue("channel_name").getAsString();

			event.deferReply().setEphemeral(true).queue(hook -> {
				MakeChannelCommand.handle(member, name, "text", null, hook);
			});
		} else if(id.equals(MODAL_APPLICATION)) {
			String url = event.getValue("channel_url").getAsString();
			ModalMapping comments = event.getValue("comments");

			event.deferReply().setEphemeral(true).queue(hook -> {
				ApplyCommand.handle(member, url,
						comments == null ? null : comments.getAsString(),
						hook);
			});
		} else if(id.startsWith(MODAL_APPLICATION_DENY)) {
			ModalMapping reason = event.getValue("reason");
			String[] args = id.split("-");

			event.deferReply().setEphemeral(true).queue(hook -> {
				ButtonContentCreatorApp.handleDenyResponse(
						member,
						args[1],
						reason == null ? null : reason.getAsString(),
						args[2],
						hook
				);
			});
		}
	}
}