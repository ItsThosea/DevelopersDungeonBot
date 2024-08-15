package me.thosea.developersdungeon.command;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class EchoCommand implements CommandHandler {
	public static final Long2ObjectOpenHashMap<EchoEntry> ECHOS = new Long2ObjectOpenHashMap<>();

	@Override
	public SlashCommandData makeCommandData() {
		return Commands.slash("echo", "Say and delete your next message through the bot.")
				.addSubcommands(new SubcommandData("message", "Echo a normal message."))
				.addSubcommands(new SubcommandData("embed", "Echo your message into an embed.")
						.addOption(OptionType.STRING, "title", "Title")
						.addOption(OptionType.STRING, "author", "Author")
						.addOption(OptionType.STRING, "author_url", "Author URL")
						.addOption(OptionType.STRING, "author_icon_url", "Author icon URL")
						.addOption(OptionType.STRING, "color", "Color (hex, R,G,B or \"random\")")
						.addOption(OptionType.STRING, "footer", "Footer")
						.addOption(OptionType.STRING, "footer_icon_url", "Footer icon URL")
						.addOption(OptionType.STRING, "thumbnail", "Thumbnail icon URL"));
	}

	@Override
	public void handle(Member member, SlashCommandInteraction event) {
		if(!Utils.isAdmin(member)) {
			event.reply("You don't have permission to do that.")
					.setEphemeral(true)
					.queue();
			return;
		}

		EchoEntry removed;
		if((removed = ECHOS.remove(member.getIdLong())) != null) {
			removed.messageDeleter.run();
		}

		BiConsumer<Member, Message> echoHandler;
		String reply;

		if("message".equalsIgnoreCase(event.getSubcommandName())) {
			echoHandler = EchoCommand::handleEchoMessage;
			reply = "your next message in this channel within 3 minutes will be deleted and echoed by me.";
		} else {
			echoHandler = makeEmbedEchoHandler(member, event);
			if(echoHandler == null) return;
			reply = "your next message in this channel within 3 minutes will be deleted and echoed by me as the description of the embed.";
		}

		event.reply(member.getAsMention() + " - " + reply)
				.setAllowedMentions(List.of())
				.queue(msg -> {
					EchoEntry entry = new EchoEntry(member, event.getChannel(),
							echoHandler,
							() -> {
								msg.deleteOriginal().queue();
							});

					ECHOS.put(member.getIdLong(), entry);
					Utils.doLater(TimeUnit.MINUTES, 3, () -> {
						if(ECHOS.remove(member.getIdLong(), entry)) {
							entry.messageDeleter.run();
						}
					});
				});
	}

	private static BiConsumer<Member, Message> makeEmbedEchoHandler(Member member, SlashCommandInteraction event) {
		Function<String, String> args = name -> event.getOption(name, OptionMapping::getAsString);

		EmbedBuilder builder = new EmbedBuilder();

		String title = args.apply("title");
		String author = args.apply("author");
		String authorUrl = args.apply("author_url");
		String authorIconUrl = args.apply("author_icon_url");
		String colorStr = args.apply("color");
		String footer = args.apply("footer");
		String footerIconUrl = args.apply("footer_icon_url");
		String thumbnail = args.apply("thumbnail");

		if(authorUrl != null && !Utils.isValidUrl(authorUrl)) {
			event.reply("Invalid author URL!").setEphemeral(true).queue();
			return null;
		} else if(authorIconUrl != null && !Utils.isValidUrl(authorIconUrl)) {
			event.reply("Invalid author icon URL!").setEphemeral(true).queue();
			return null;
		} else if(footerIconUrl != null && !Utils.isValidUrl(footerIconUrl)) {
			event.reply("Invalid footer icon URL!").setEphemeral(true).queue();
			return null;
		} else if(thumbnail != null && !Utils.isValidUrl(thumbnail)) {
			event.reply("Invalid thumbnail URL!").setEphemeral(true).queue();
			return null;
		}

		builder.setTitle(title);
		builder.setAuthor(author, authorUrl, authorIconUrl);
		builder.setThumbnail(thumbnail);

		if(colorStr != null) {
			Color color = Utils.parseColor(colorStr, event);
			if(color == null) return null;
		}

		builder.setFooter(footer, footerIconUrl);
		return (_, og) -> {
			builder.setDescription(og.getContentRaw());

			og.getChannel()
					.sendMessageEmbeds(builder.build())
					.setAllowedMentions(List.of())
					.queue(ourMsg -> {
						Utils.logMinor("%s used echo (embed) in %s > %s", member, og.getChannel(), ourMsg);
						og.delete().queue();
					});
		};
	}

	private static void handleEchoMessage(Member member, Message og) {
		List<Attachment> attachments = og.getAttachments();
		List<FileUpload> uploads = new ArrayList<>(attachments.size());
		for(Attachment attach : attachments) {
			try {
				uploads.add(FileUpload.fromData(
						attach.getProxy().download().get(),
						attach.getFileName()));
			} catch(Exception e) {
				return;
			}
		}

		og.getChannel()
				.sendMessage(og.getContentRaw())
				.setEmbeds(og.getEmbeds())
				.setAllowedMentions(List.of())
				.addFiles(uploads)
				.queue(ourMsg -> {
					Utils.logMinor("%s used echo (message) in %s > %s", member, og.getChannel(), ourMsg);
					og.delete().queue();
				});
	}

	public record EchoEntry(Member member, MessageChannelUnion channel,
	                        BiConsumer<Member, Message> echoHandler,
	                        Runnable messageDeleter) {}
}