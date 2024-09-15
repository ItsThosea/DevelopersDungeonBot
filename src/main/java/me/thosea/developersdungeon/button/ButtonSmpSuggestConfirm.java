package me.thosea.developersdungeon.button;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.command.SmpSuggestionCommand;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.messages.MessagePollData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ButtonSmpSuggestConfirm implements ButtonHandler {
	private static final Path SUGGESTED_MODS_FILE = Paths.get("./devdungeon_suggested_mods.json");
	private static final Executor FILE_READER_SERVICE = Executors.newSingleThreadExecutor();

	@Override
	public String getId() {
		return ButtonHandler.ID_SMP_SUGGEST_CONFIRM;
	}

	@Override
	public void handle(Member member, ButtonInteractionEvent event, String[] args) {
		MessageEmbed embed = event.getMessage().getEmbeds().getFirst();
		List<Field> fields = embed.getFields();
		boolean irresistible = Boolean.parseBoolean(args[2]);

		if(args[0].equals("deny")) {
			event.reply("Then so be it.")
					.setEphemeral(true)
					.and(event.getMessage().delete())
					.queue(_ -> {}, _ -> {});
			return;
		} else if(args[0].equals("retype")) {
			event.replyModal(SmpSuggestionCommand.createModal(
							args[1],
							fields.get(0).getValue(),
							fields.get(1).getValue(),
							irresistible
					))
					.and(event.getMessage().delete())
					.queue(_ -> {}, _ -> {});
			return;
		}

		Utils.logMinor(
				"%s submitted SMP %s suggestion form (Content: %s, Reason: %s)",
				member, args[1],
				fields.get(0).getValue(),
				fields.get(1).getValue()
		);

		String type = args[1];

		event.deferReply().setEphemeral(true).queue(hook -> {
			if((type.equals("mod") || type.equals("pack"))) {
				String url = embed.getFields().getFirst().getValue();
				if(!Utils.isValidUrl(url)) {
					hook.editOriginal("That is not a valid URL.").queue();
					return;
				}

				FILE_READER_SERVICE.execute(() -> {
					try {
						readFile(url, event.getMessage(), irresistible, type, hook);
					} catch(Exception e) {
						System.err.println("Error reading file " + SUGGESTED_MODS_FILE.toAbsolutePath());
						e.printStackTrace();
						hook.editOriginal("Error reading JSON file. Please contact an admin.").queue();
						Utils.logMajor(
								"Error reading suggested mods JSON file: %s. \nRead console for more.",
								e.toString()
						);
					}
				});
			} else {
				String title = type.equals("config")
						? "Config Suggestion - " + fields.get(0).getValue()
						: member.getEffectiveName() + "'s Suggestion";
				sendMessage(event.getMessage(), hook, irresistible, title, null);
			}
		});

	}

	private static final List<String> URL_KEYWORDS_MOD = List.of(
			"curseforge.com/minecraft/mc-mods/",
			"modrinth.com/mod/"
	);
	private static final List<String> URL_KEYWORDS_PACK = List.of(
			"curseforge.com/minecraft/texture-packs/",
			"curseforge.com/minecraft/data-packs/",
			"curseforge.com/minecraft/shaders/",
			"modrinth.com/resourcepack/",
			"modrinth.com/datapack/",
			"modrinth.com/shader/"
	);
	private static final String MOD_BAD_URL = "Not a link to a CurseForge or Modrinth mod page.";
	private static final String PACK_BAD_URL = "Not a link to a CurseForge/Modrinth resourcepack, datapack or shaderpack.";

	private void readFile(String url, Message msg,
	                      boolean irresistible, String type,
	                      InteractionHook hook) throws Exception {
		boolean isMod = type.equals("mod");
		Iterator<String> keywordIterator = (isMod ? URL_KEYWORDS_MOD : URL_KEYWORDS_PACK).iterator();
		String keyword;
		int index;
		do {
			if(!keywordIterator.hasNext()) {
				hook.editOriginal(isMod ? MOD_BAD_URL : PACK_BAD_URL).queue();
				return;
			}
			keyword = keywordIterator.next();
			index = url.indexOf(keyword);
		} while(index == -1);

		String modid = url.substring(index + keyword.length()).split("/")[0];
		if(modid.isBlank()) {
			hook.editOriginal("Invalid mod ID.").queue();
			return;
		}

		String title = (type.equals("mod") ? "Mod" : "Pack") + " Suggestion - " + modid;

		if(irresistible) {
			sendMessage(msg, hook, true, title, null);
			return;
		}

		JsonObject object;
		if(!Files.exists(SUGGESTED_MODS_FILE)) {
			object = new JsonObject();
		} else {
			object = Utils.GSON.fromJson(Files.readString(SUGGESTED_MODS_FILE), JsonObject.class);
		}

		if(object.get(modid) instanceof JsonPrimitive link) {
			hook.editOriginal("This was already suggested: " + link.getAsString()).queue();
		} else {
			sendMessage(msg, hook, false, title, msgUrl -> {
				object.addProperty(modid, msgUrl);
				try {
					Files.writeString(SUGGESTED_MODS_FILE, Utils.GSON.toJson(object));
				} catch(IOException e) {
					System.err.println("Error writing to file " + SUGGESTED_MODS_FILE.toAbsolutePath());
					e.printStackTrace();
					hook.editOriginal("Suggestion submitted > " + msgUrl
									+ "\nAdditionally, failed to write to the JSON. Please contact an admin.")
							.queue();
					Utils.logMajor(
							"Failed to write to suggested mods JSON: %s.\nRead console for more.",
							e.toString()
					);
				}
			});
		}
	}

	private void sendMessage(Message msg, InteractionHook hook,
	                         boolean irresistible, String title, Consumer<String> urlHandler) {
		assert Main.votingChannel != null; // checked when running command

		Main.votingChannel
				.sendMessageEmbeds(msg.getEmbeds().getFirst())
				.setPoll(MessagePollData.builder(title)
						.addAnswer("Yes", Utils.EMOJI_YES)
						.addAnswer(
								irresistible ? "Definitely!" : "Nuh uh!",
								irresistible ? Utils.EMOJI_SMILE : Utils.EMOJI_NO
						)
						.setDuration(1, TimeUnit.DAYS)
						.build())
				.queue(voteMsg -> {
					msg.delete().queue();
					String url = voteMsg.getJumpUrl();
					hook.editOriginal("Suggestion submitted > " + url).queue();
					if(urlHandler != null) urlHandler.accept(url);
				});
	}
}