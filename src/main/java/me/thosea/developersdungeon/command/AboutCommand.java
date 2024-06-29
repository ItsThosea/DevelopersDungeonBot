package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.Main;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public class AboutCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("about", "Who am I?");
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		event.reply("""
						I am the Dungeon Keeper, a consciousness beyond your understanding created by %s with JDA.
						I manage private channels; I make team roles; I log messages.
						I am on version %s.
						""".formatted("<@959062384419410011>", Main.VERSION))
				.setAllowedMentions(List.of())
				.setEphemeral(true)
				.queue();
	}
}
