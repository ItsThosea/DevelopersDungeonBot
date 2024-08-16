package me.thosea.developersdungeon.command;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.button.ButtonHandler;
import me.thosea.developersdungeon.event.ModalResponseListener;
import me.thosea.developersdungeon.util.Constants.Channels;
import me.thosea.developersdungeon.util.Constants.Roles;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.Color;
import java.util.Collection;
import java.util.List;

public class ApplyCommand implements CommandHandler {
	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("apply", "Apply for the Content Creator role!");
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		if(Roles.CONTENT_CREATOR < 0 || Channels.APPLICATIONS < 0) {
			event.reply("Not setup!").setEphemeral(true).queue();
			return;
		} else if(Utils.hasRole(member, Roles.CONTENT_CREATOR)) {
			event.reply("You already have the <@&" + Roles.CONTENT_CREATOR + "> role!")
					.setEphemeral(true)
					.setAllowedMentions(List.of())
					.queue();
			return;
		}

		TextInput url = TextInput.create("channel_url", "Channel URL", TextInputStyle.SHORT)
				.setPlaceholder("YouTube, TikTok, etc...")
				.setRequiredRange(5, 1000)
				.build();
		TextInput comment = TextInput.create("comments", "Comments", TextInputStyle.PARAGRAPH)
				.setPlaceholder("(Optional) Additional comments")
				.setRequiredRange(0, 2000)
				.setRequired(false)
				.build();

		event.replyModal(Modal.create(ModalResponseListener.MODAL_APPLICATION, "Content Creator Application")
						.addComponents(ActionRow.of(url))
						.addComponents(ActionRow.of(comment))
						.build())
				.queue();
		Utils.logMinor("%s requested content creator application form", member);
	}

	public static void handle(Member member, String url, String comments, InteractionHook hook) {
		Utils.logMinor("%s submitted application form (Url: %s, Comments: %s)", member, url, comments);

		if(Utils.hasRole(member, Roles.CONTENT_CREATOR)) {
			hook.editOriginal("You already have the role!").queue();
			return;
		}

		TextChannel channel = Main.guild.getTextChannelById(Channels.APPLICATIONS);
		if(channel == null) {
			hook.editOriginal("There is no applications channel!").queue();
			return;
		}

		channel.sendMessageEmbeds(makeAppEmbed(member, url, comments))
				.setAllowedMentions(List.of())
				.setActionRow(makeActionRow(member, true))
				.queue(_ -> {
					member.getUser().openPrivateChannel().queue(dm -> {
						dm.sendMessageEmbeds(makeDmInitEmbed(member.getColorRaw())).queue(_ -> {
							hook.editOriginal("Your application has been sent. Check your DMs for updates.").queue();
						}, _ -> {
							hook.editOriginal("Your application was sent, but the DM failed. Turn on DMs to see updates.").queue();
						});
					});
				});

		hook.editOriginal("Your application has been sent.").queue();
	}

	private static MessageEmbed makeAppEmbed(Member member, String url, String comment) {
		return new EmbedBuilder()
				.setColor(member.getColorRaw())
				.setTitle("New Application")
				.setDescription("New application from " + member.getAsMention() + ":")
				.addField("Channel URL", url, false)
				.addField("User Comments", comment == null || comment.isBlank() ? "(None)" : comment, false)
				.build();
	}

	private static MessageEmbed makeDmInitEmbed(int color) {
		return new EmbedBuilder()
				.setColor(color)
				.setTitle("Application Sent")
				.setDescription("Your content creator application has been sent.\nYou'll be DM'd when it's accepted or denied.")
				.build();
	}

	public static MessageEmbed makeDmAcceptEmbed() {
		return new EmbedBuilder()
				.setColor(new Color(21, 230, 94))
				.setTitle("Application Accepted")
				.setDescription("Your content creator application has been accepted.\nYou now have the Content Creator role!")
				.build();
	}

	public static MessageEmbed makeDmDenyEmbed(String reason) {
		return new EmbedBuilder()
				.setColor(new Color(209, 40, 31))
				.setTitle("Application Denied")
				.setDescription("Your content creator application has been denied.")
				.appendDescription((reason == null ? "" : "\nReason: " + reason))
				.build();
	}

	public static Collection<ItemComponent> makeActionRow(Member member, boolean enabled) {
		String suffix = member == null ? "" : "-" + member.getId();

		var yes = Button.success(
				ButtonHandler.ID_CONTENT_CREATOR_APP + "-accept" + suffix,
				"Accept");
		var no = Button.danger(
				ButtonHandler.ID_CONTENT_CREATOR_APP + "-deny" + suffix,
				"Deny");
		var delete = Button.secondary(
				ButtonHandler.ID_CONTENT_CREATOR_APP + "-delete",
				"Delete");

		if(!enabled) {
			yes = yes.asDisabled();
			no = no.asDisabled();
		}
		return List.of(yes, no, delete);
	}
}