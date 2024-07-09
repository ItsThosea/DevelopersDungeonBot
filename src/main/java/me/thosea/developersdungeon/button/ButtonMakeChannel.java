package me.thosea.developersdungeon.button;

import me.thosea.developersdungeon.event.ModalResponseListener;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class ButtonMakeChannel implements ButtonHandler {
	@Override
	public String getId() {
		return ButtonHandler.ID_MAKE_CHANNEL;
	}

	@Override
	public void handle(Member member, ButtonInteractionEvent event, String[] args) {
		if(!args[0].equals(member.getId())) {
			event.reply("Only the original poster can do this.")
					.setEphemeral(true)
					.queue();
			return;
		}

		TextInput nameField = TextInput.create("channel_name", "Channel Name", TextInputStyle.SHORT)
				.setPlaceholder("You can change this later.")
				.setRequiredRange(1, 100)
				.build();

		event.replyModal(Modal.create(ModalResponseListener.MODAL_MAKE_CHANNEL, "Make-A-Channel")
						.addComponents(ActionRow.of(nameField))
						.build())
				.queue();
	}
}
