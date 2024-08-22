package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.event.ModalResponseListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.Locale;

import static me.thosea.developersdungeon.button.ButtonHandler.ID_SMP_SUGGEST_CONFIRM;

public class SmpSuggestionCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("smpsuggestion", "Make a suggestion to the SMP.")
				.addSubcommands(new SubcommandData("mod", "Suggest a mod to be added to the SMP."))
				.addSubcommands(new SubcommandData("config", "Suggest a config change to the SMP."))
				.addSubcommands(new SubcommandData("resourcepack", "Suggest a resourcepack change to the SMP."))
				.addSubcommands(new SubcommandData("other", "Suggest something else to the SMP."));
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		if(Main.votingChannel == null) {
			event.reply("This command is disabled.").setEphemeral(true).queue();
			return;
		}

		String type = event.getSubcommandName();
		assert type != null;
		event.replyModal(createModal(type, null, null)).queue();
	}

	public static Modal createModal(String type, String prevContent, String prevReason) {
		TextInput content = TextInput.create("content",
						(type.equals("mod") ? "Mod URL" : "Change wanted"),
						TextInputStyle.SHORT)
				.setRequiredRange(3, 300)
				.setPlaceholder(prevContent == null
						? (type.equals("mod") ?  "Duplicate mod URLs will be auto-blocked." : null)
						: "Previous: " + prevContent)
				.build();
		TextInput reason = TextInput.create("reason", "Reasoning", TextInputStyle.PARAGRAPH)
				.setPlaceholder(prevReason == null ? null : "Previous: " + prevReason)
				.setRequiredRange(3, 500)
				.build();

		return Modal.create(
				ModalResponseListener.MODAL_SMP_SUGGESTION + "-" + type,
				"SMP suggestion (" + type + ")"
		).addActionRow(content).addActionRow(reason).build();
	}

	public static void handleModalResponse(Member member, String type,
	                                       String content, String reason,
	                                       InteractionHook hook) {
		MessageEmbed embed = new EmbedBuilder()
				.setAuthor(member.getUser().getName(), "https://curseforge.com/minecraft/mc-mods/badoptimizations", member.getUser().getAvatarUrl())
				.setColor(member.getColorRaw())
				.setTitle(type.substring(0, 1).toUpperCase(Locale.ENGLISH) + type.substring(1) + " Suggestion")
				.addField(
						type.equals("mod") ? "Mod URL" : "Change wanted",
						content, false
				)
				.addField("Reasoning", reason, false)
				.build();

		hook.editOriginal("Confirm suggestion?")
				.setEmbeds(embed)
				.setActionRow(
						Button.success(ID_SMP_SUGGEST_CONFIRM + "-accept-" + type, "Yes"),
						Button.secondary(ID_SMP_SUGGEST_CONFIRM + "-retype-" + type, "Retype"),
						Button.danger(ID_SMP_SUGGEST_CONFIRM + "-deny-" + type, "Nevermind"))
				.queue();
	}
}