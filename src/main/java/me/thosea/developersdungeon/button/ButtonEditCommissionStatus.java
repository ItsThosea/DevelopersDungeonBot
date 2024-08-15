package me.thosea.developersdungeon.button;

import me.thosea.developersdungeon.event.ModalResponseListener;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class ButtonEditCommissionStatus implements ButtonHandler{
	@Override
	public String getId() {
		return ButtonHandler.ID_EDIT_STATUS;
	}

	@Override
	public void handle(Member member, ButtonInteractionEvent event, String[] args) {
		TextInput statusField = TextInput.create("status", "Status", TextInputStyle.SHORT)
				.setPlaceholder("No special characters allowed.")
				.setRequiredRange(1, 50)
				.build();

		event.replyModal(Modal.create(ModalResponseListener.MODAL_EDIT_STATUS, "Edit Status")
						.addComponents(ActionRow.of(statusField))
						.build())
				.queue();
	}
}