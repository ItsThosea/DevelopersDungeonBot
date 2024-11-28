package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.button.ButtonHandler;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Map;
import java.util.UUID;

public class ButtonListener extends ListenerAdapter {
	public static final Map<String, ButtonHandler> BUTTONS = ButtonHandler.makeHandlers();

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		if(!event.getMessage().getAuthor().equals(Main.jda.getSelfUser())) return;

		Member member = event.getMember();
		if(member == null) return;

		String fullId = event.getButton().getId();
		if(fullId == null) return;
		if(fullId.startsWith("devdungeon_debug_button")) return;

		int index = fullId.indexOf('-');

		String[] args = index == -1 || index + 1 == fullId.length()
				? new String[0]
				: fullId.substring(index + 1).split("-");
		String id = index == -1 ? fullId : fullId.substring(0, index);

		ButtonHandler handler = BUTTONS.get(id);
		if(handler != null) {
			handler.handle(member, event, args);
		} else {
			event.reply("Invalid button").setEphemeral(true).queue();
		}
	}

	public static String createDebugButtonId() {
		return "devdungeon_debug_button_" + UUID.randomUUID();
	}
}