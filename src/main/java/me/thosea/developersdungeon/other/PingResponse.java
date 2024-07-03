package me.thosea.developersdungeon.other;

import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface PingResponse {
	List<PingResponse> PIPELINE = List.of(
			normal("3.14", "I HATE Pi!!!"),
			normal(List.of("curseforge", "modrinth"), "CurseForge? Modrinth? Huh? Only thing I care about is <https://curseforge.com/minecraft/mc-mods/badoptimizations> ;)"),
			normal("i fell", "...from the light?"),
			normal("wonder", "https://tenor.com/view/wonder-effect-super-mario-wonder-mario-irony-talking-flower-mario-bros-meme-gif-4164277542385047547"),
			normal("walls", "https://tenor.com/view/im-in-your-walls-gif-25753367"),
			normal("happy", "https://tenor.com/view/animatic-battle-animatic-frolick-fun-play-gif-18082323931657388533"),
			normal("hair", "Thanks for asking, I use dungeon-metal hair gel."),
			normal("everyone", content -> {
				return content.contains("dont")
						? "Pfft, I'd never! Right...?"
						: "I can ping everyone, just depends on how many curseforge points you have.";
			}),

			progressive("hi", "Hi!"),
			progressive("hello", "Hello!"),
			progressive("..."),
			progressive("!!?*%^@das@?!@"),
			progressive(content -> {
				long time = System.currentTimeMillis();
				String[] split = content.split(" ");
				for(String str : split) {
					try {
						return Math.abs(time - Long.parseLong(str)) <= 2629746000L;
					} catch(NumberFormatException ignored) {}
				}

				return false;
			}, () -> "" + System.currentTimeMillis()),
			progressive("bedrock", "Beetroot seeds came to Java or Bedrock first?"),
			progressive("bane of arthropods", "What enchantment can give spiders slowness?"),
			progressive("1.18", "In what Bedrock version did wood become impossible to obtain in flat worlds without the bonus chest?"),
			progressive("20100219", "Until what indev version did logs break faster than stone?"),
			progressive("help", "Help me..."),
			progressive("ouch", "Ouch..."),
			progressive("celestial", "No celestial calamity could ever even match this."),
			progressive("jamma", "Now if only Jamma was here..."),
			progressive("grapplecaster", "Grapplecaster.", "grapplecaster.png"),
			progressive("windmaker", "Windmaker.", "windmaker.png"),
			progressive("feline foe", "Feline Foe.", "feline_foe.png"),
			finalProgressive(msg -> {
				if(msg.contains("you win")) {
					return "i win";
				} else if(msg.contains("i win")) {
					return "i lose :(";
				} else {
					return "Ok I give up you win";
				}
			})
	);

	boolean mustMatch();
	boolean matches(String content);
	String getResponse(String content);
	default MessageCreateAction modifyMessage(MessageCreateAction action) {
		return action;
	}

	static PingResponse normal(String keyword, String response) {
		return new PingResponse() {
			@Override public boolean mustMatch() {return true;}

			@Override
			public boolean matches(String content) {
				return content.contains(keyword);
			}
			@Override
			public String getResponse(String content) {
				return response;
			}
		};
	}

	static PingResponse normal(List<String> keywords, String response) {
		return new PingResponse() {
			@Override public boolean mustMatch() {return true;}

			@Override
			public boolean matches(String content) {
				return keywords.stream().anyMatch(content::contains);
			}
			@Override
			public String getResponse(String content) {
				return response;
			}
		};
	}

	static PingResponse normal(String keyword, Function<String, String> response) {
		return new PingResponse() {
			@Override public boolean mustMatch() {return true;}

			@Override
			public boolean matches(String content) {
				return content.contains(keyword);
			}
			@Override
			public String getResponse(String content) {
				return response.apply(content);
			}
		};
	}

	static PingResponse progressive(String response) {
		return progressive(response, response);
	}

	static PingResponse progressive(String keyword, String response) {
		return progressive(keyword, response, null);
	}

	static PingResponse progressive(String keyword, String response, @Nullable String resource) {
		return progressive(content -> content.contains(keyword), () -> response, resource);
	}

	static PingResponse progressive(Function<String, Boolean> keyword, Supplier<String> response) {
		return progressive(keyword, response, null);
	}

	static PingResponse progressive(Function<String, Boolean> keyword,
	                                Supplier<String> response,
	                                @Nullable String file) {
		return new PingResponse() {
			@Override public boolean mustMatch() {return false;}

			@Override
			public boolean matches(String content) {
				return keyword.apply(content);
			}
			@Override
			public String getResponse(String content) {
				return response.get();
			}

			@Override
			public MessageCreateAction modifyMessage(MessageCreateAction action) {
				if(file != null) {
					Utils.loadResource(file, stream -> {
						byte[] buffer = stream.readAllBytes();
						var byteStream = new ByteArrayInputStream(buffer);
						action.addFiles(FileUpload.fromData(byteStream, "image.png"));
					});
				}

				return action;
			}
		};
	}

	static PingResponse finalProgressive(Function<String, String> response) {
		return new PingResponse() {
			@Override public boolean mustMatch() {return true;}
			@Override public boolean matches(String content) {return true;}
			@Override public String getResponse(String content) {return response.apply(content);}
		};
	}
}
