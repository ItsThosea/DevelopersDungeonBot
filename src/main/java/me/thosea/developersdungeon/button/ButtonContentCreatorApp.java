package me.thosea.developersdungeon.button;

import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.command.ApplyCommand;
import me.thosea.developersdungeon.event.ModalResponseListener;
import me.thosea.developersdungeon.util.Constants.Roles;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class ButtonContentCreatorApp implements ButtonHandler {
	@Override
	public String getId() {
		return ButtonHandler.ID_CONTENT_CREATOR_APP;
	}

	@Override
	public void handle(Member member, ButtonInteractionEvent event, String[] args) {
		if(!Utils.isAdmin(member)) {
			event.reply("You can't do that!").setEphemeral(true).queue();
			return;
		}

		String action = args[0];
		if(action.equals("delete")) {
			handleDelete(event);
			return;
		} else if(action.equals("deny")) {
			handleDeny(event, args[1]);
			return;
		} else if(!action.equals("accept")) {
			event.reply("Invalid action: " + action).setEphemeral(true).queue();
			return;
		}

		event.deferReply().setEphemeral(true).queue(hook -> Main.guild.retrieveMemberById(args[1]).queue(target -> {
			handleAccept(event.getMember(), hook, target, event.getMessage());
		}, _ -> {
			hook.editOriginal("The user is not in the server.").queue();
		}));
	}

	private void handleDelete(ButtonInteractionEvent event) {
		event.deferReply().setEphemeral(true).queue(hook -> {
			event.getMessage().delete().queue(_ -> {
				hook.editOriginal("Deleted the message.").queue();
			}, _ -> {
				hook.editOriginal("The message was already deleted.").queue();
			});
		});
	}

	private void handleAccept(Member member, InteractionHook hook, Member target, Message msg) {
		Role role = Main.guild.getRoleById(Roles.CONTENT_CREATOR);
		if(role == null) {
			hook.editOriginal("There is no content creator role!").queue();
			return;
		} else if(Utils.hasRole(target, role)) {
			hook.editOriginal("They already have the role!").queue();
			return;
		}

		Main.guild.addRoleToMember(target, role).queue();

		Utils.logMinor("%s accepted %s's content creator application: %s", member, target, msg);
		msg.editMessageEmbeds(new EmbedBuilder(msg.getEmbeds().getFirst())
						.addField("Accepted by @" + member.getUser().getName(), "", false)
						.build())
				.setActionRow(ApplyCommand.makeActionRow(target, false))
				.queue();
		target.getUser().openPrivateChannel()
				.flatMap(channel -> channel.sendMessageEmbeds(ApplyCommand.makeDmAcceptEmbed()))
				.queue(_ -> {
					hook.editOriginal("They now have the " + role.getAsMention() + " role! (DM success)").queue();
				}, _ -> {
					hook.editOriginal("They now have the " + role.getAsMention() + " role! (But the DM failed...)").queue();
				});
	}

	private void handleDeny(ButtonInteractionEvent event, String targetId) {
		TextInput reason = TextInput.create("reason", "Reason", TextInputStyle.SHORT)
				.setPlaceholder("Optional denial reason (shown to the user)")
				.setRequiredRange(0, 2000)
				.setRequired(false)
				.build();

		event.replyModal(Modal.create(ModalResponseListener.MODAL_APPLICATION_DENY
										+ "-" + event.getMessage().getId()
										+ "-" + targetId,
								"Deny Application")
						.addComponents(ActionRow.of(reason))
						.build())
				.queue();
	}

	public static void handleDenyResponse(Member member, String messageId,
	                                      String reason, String targetId,
	                                      InteractionHook hook) {
		if(!Utils.isAdmin(member)) {
			hook.editOriginal("You are no longer OP!").queue();
			return;
		}

		hook.getInteraction().getMessageChannel().retrieveMessageById(messageId).queue(msg -> {
			Utils.logMinor("%s denied <@%s>'s content creator application because %s: %s", member, targetId, reason, msg);

			msg.editMessageEmbeds(new EmbedBuilder(msg.getEmbeds().getFirst())
							.addField("Denied by @" + member.getUser().getName(),
									"Reason: " + (reason == null || reason.isBlank() ? "None" : reason),
									false)
							.build())
					.setActionRow(ApplyCommand.makeActionRow(null, false))
					.queue();

			Main.guild.retrieveMemberById(targetId).queue(target -> {
				target.getUser().openPrivateChannel()
						.flatMap(channel -> channel.sendMessageEmbeds(ApplyCommand.makeDmDenyEmbed(reason)))
						.queue(_ -> {
							hook.editOriginal("Denied. (DM success)").queue();
						}, _ -> {
							hook.editOriginal("Denied. (But the DM failed...)").queue();
						});
			}, _ -> {
				hook.editOriginal("Denied. No DM was sent because they left the server.").queue();
			});
		}, _ -> {
			hook.editOriginal("The message was deleted!").queue();
		});
	}
}