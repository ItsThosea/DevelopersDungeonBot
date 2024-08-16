package me.thosea.developersdungeon.button;

import me.thosea.developersdungeon.command.TeamCommand;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class ButtonTeamListPage implements ButtonHandler {
	@Override
	public String getId() {
		return ButtonHandler.ID_TEAM_LIST_PAGE;
	}

	@Override
	public void handle(Member member, ButtonInteractionEvent event, String[] args) {
		if(!args[2].equals(member.getId()) && !Utils.isAdmin(member)) {
			event.reply("You can't click that!").setEphemeral(true).queue();
			return;
		}

		event.getMessage().editMessageComponents(ActionRow.of(event.getMessage()
				.getComponents()
				.get(0)
				.getButtons()
				.stream()
				.map(Button::asDisabled)
				.toList())
		).queue();

		event.deferReply().setEphemeral(true).queue(hook -> {
			TeamCommand.handleList(
					member.getId(),
					event.getMessage().editMessage(""),
					action -> {
						action.queue();
						hook.deleteOriginal().queue();
					},
					Integer.parseInt(args[0]),
					Boolean.parseBoolean(args[1]));
		});
	}
}