package me.thosea.developersdungeon.event.button;

import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.List;

public class ButtonDenyTeamRequest implements ButtonHandler {
	@Override
	public String getId() {
		return ButtonHandler.ID_DENY_TEAM_REQUEST;
	}

	@Override
	public void handle(Member member, ButtonInteractionEvent event, String[] args) {
		String id = member.getId();

		String fromOrTo;
		String end;
		if(id.equals(args[1])) { // target clicked button
			fromOrTo = "from";
			end = args[2];
		} else if(id.equals(args[2])) { // requester clicked button
			fromOrTo = "to";
			end = args[1];
		} else {
			event.reply("You can't do that!").setEphemeral(true).queue();
			return;
		}

		var message = event.getMessage();
		var msg = "%s cancelled their team %s request %s <@%s>.".formatted(
				member.getAsMention(), args[0],
				fromOrTo, end);

		Utils.logMinor(msg);
		message.editMessage(msg)
				.setAllowedMentions(List.of())
				.setComponents()
				.queue();
		event.reply("The request has been cancelled.").setEphemeral(true).queue();
	}
}
