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
				.addSubcommands(new SubcommandData("pack", "Suggest a resource, data or shader pack change to the SMP."))
				.addSubcommands(new SubcommandData("other", "Suggest something else, like a mod removal or rule change, to the SMP."));
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		if(Main.votingChannel == null) {
			event.reply("This command is disabled.").setEphemeral(true).queue();
			return;
		}

		String type = event.getSubcommandName();
		assert type != null;
		event.replyModal(createModal(type, null, null, false)).queue();
	}

	public static Modal createModal(String type, String prevContent, String prevReason,
	                                boolean irresistible) {
		String contentPrompt;
		String contentPlaceholder;
		if(type.equals("mod")) {
			contentPrompt = "Mod URL";
			contentPlaceholder = "Duplicate mod URLs will be auto-blocked.";
		} else if(type.equals("pack")) {
			contentPrompt = "Pack URL";
			contentPlaceholder = "Duplicate pack URLs will be auto-blocked.";
		} else {
			contentPrompt = "Change Wanted";
			contentPlaceholder = null;
		}

		TextInput content = TextInput.create("content", contentPrompt, TextInputStyle.SHORT)
				.setRequiredRange(3, 300)
				.setPlaceholder(prevContent == null ? contentPlaceholder : getPlaceholder(prevContent))
				.build();
		TextInput reason = TextInput.create("reason", "Reasoning", TextInputStyle.PARAGRAPH)
				.setPlaceholder(getPlaceholder(prevReason))
				.setRequiredRange(3, 500)
				.build();

		return Modal.create(
				ModalResponseListener.MODAL_SMP_SUGGESTION + "-" + type + "-" + irresistible,
				"SMP suggestion (" + type + ")"
		).addActionRow(content).addActionRow(reason).build();
	}

	private static String getPlaceholder(String prev) {
		if(prev == null) {
			return null;
		} else {
			String prefix = "Previous: ";
			int maxLength = 100 - prefix.length();
			if(prev.length() > maxLength) {
				prev = prev.substring(0, maxLength - 3) + "...";
			}
			return prefix + prev;
		}
	}

	public static void handleModalResponse(Member member, String type,
	                                       String content, String reason,
	                                       InteractionHook hook,
	                                       boolean irresistible) {
		MessageEmbed embed = new EmbedBuilder()
				.setAuthor(member.getUser().getName(), null, member.getUser().getAvatarUrl())
				.setColor(member.getColorRaw())
				.setTitle(type.substring(0, 1).toUpperCase(Locale.ENGLISH) + type.substring(1) + " Suggestion")
				.addField(
						type.equals("mod") ? "Mod URL" : "Change wanted",
						content, false
				)
				.addField("Reasoning", reason, false)
				.build();

		String idSuffix = type + "-" + irresistible;
		hook.editOriginal(!irresistible ? "Confirm suggestion?" : "Confirm ||irresistible|| suggestion?")
				.setEmbeds(embed)
				.setActionRow(
						Button.success(ID_SMP_SUGGEST_CONFIRM + "-accept-" + idSuffix, "Yes"),
						Button.secondary(ID_SMP_SUGGEST_CONFIRM + "-retype-" + idSuffix, "Retype"),
						Button.danger(ID_SMP_SUGGEST_CONFIRM + "-deny-" + idSuffix, "Nevermind"))
				.queue();
	}
}