package me.thosea.developersdungeon.event;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.event.button.ButtonHandler;
import me.thosea.developersdungeon.util.Constants;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class ButtonListener extends ListenerAdapter {
	public static final Map<String, ButtonHandler> BUTTONS = ButtonHandler.makeHandlers();

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		if(!event.getMessage().getAuthor().equals(Main.jda.getSelfUser())) return;

		Member member = event.getMember();
		if(member == null) return;

		String fullId = event.getButton().getId();
		if(fullId == null) return;

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

	private static int nextDebugId = 0;

	public static void doDebugMessage(SlashCommandInteraction event,
	                                  String text,
	                                  boolean ephemeral,
	                                  Function<String, Button> buttonFunction,
	                                  Consumer<InteractionHook> handler) {
		String id = "devdungeon_debug" + nextDebugId;
		nextDebugId++;

		BUTTONS.put(id, new ButtonHandler() {
			@Override public String getId() {return id;}

			@Override
			public void handle(Member member, ButtonInteractionEvent event, String[] args) {
				if(!Constants.ADMINS.contains(member.getIdLong())) {
					event.reply("You can't do that!").setEphemeral(true).queue();
					return;
				}

				event.deferReply().setEphemeral(ephemeral).queue(handler);
			}
		});

		event.reply(text)
				.setEphemeral(ephemeral)
				.setActionRow(buttonFunction.apply(id))
				.queue(msg -> {
					Utils.doLater(TimeUnit.SECONDS, 30, () -> {
						msg.deleteOriginal().queue(i_ -> {}, err -> {});
						BUTTONS.remove(id);
					});
				});
	}

}