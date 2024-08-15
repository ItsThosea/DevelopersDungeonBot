package me.thosea.developersdungeon.button;

import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class ButtonDeletePChannel implements ButtonHandler {
	@Override
	public String getId() {
		return ButtonHandler.ID_PCHANNEL_DELETE;
	}

	@Override
	public void handle(Member member, ButtonInteractionEvent event, String[] args) {
		if(!member.getId().equals(args[0]) && !Utils.isAdmin(member)) {
			event.reply("You can't do that...anymore!").setEphemeral(true).queue();
			return;
		}

		Utils.logChannel("%s deleted private channel %s", member, event.getChannel());
		event.getChannel().delete().queue();
		// No need to respond to the event.
	}
}