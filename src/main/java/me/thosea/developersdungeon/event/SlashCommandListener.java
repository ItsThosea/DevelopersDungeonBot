package me.thosea.developersdungeon.event;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.thosea.developersdungeon.command.CommandHandler;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

public class SlashCommandListener extends ListenerAdapter {
	private static final LongSet COOLDOWNS = new LongOpenHashSet();

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if(event.getMember() == null) {
			// Never runs, commands are guild-only
			return;
		}

		CommandHandler handler = CommandHandler.COMMANDS.get(event.getName());

		if(handler != null) {
			long id = event.getMember().getIdLong();
			if(COOLDOWNS.contains(id)) {
				// Spamming messes with some commands
				event.reply("You can't execute commands that fast!")
						.setEphemeral(true)
						.queue();
				return;
			}

			COOLDOWNS.add(id);
			Utils.doLater(TimeUnit.MILLISECONDS, 2000L, () -> COOLDOWNS.remove(id));

			handler.handle(event.getMember(), event.getInteraction());
		}
	}
}