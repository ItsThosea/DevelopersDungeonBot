package me.thosea.developersdungeon.other;

import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;

public interface PingResponse {
	boolean[] frolicState = {false};
	boolean[] dancingState = {true};

	List<PingResponse> PIPELINE = List.of(
			normal("3.14", "I HATE Pi!!!"),
			normal(List.of("curseforge", "modrinth"), "CurseForge? Modrinth? Huh? Only thing I care about is <https://curseforge.com/minecraft/mc-mods/badoptimizations> ;)"),
			normal("badoptimizations", "How about \"Good Optimizations\"?"),
			normal(List.of("thosea", "959062384419410011"), "Sounds hot."),
			normal(List.of("tinygames", "tiny games", "483056015890186263"), "What about \"big games\"?"),
			normal("hackermanz", "[hackermanz]($cdn/mp4/hackermanz.mp4)"),
			normal("mrbeast", "[$$$]($cdn/mp4/mrbeast.mp4)"),
			normal("femboy", "I'm not one, but he may be..[.]($cdn/png/femboy.png)"),
			normal("i fell", "...from the light? Talk or will you fight?"),
			normal("yarn", "Yarn mentioned. Exterminating user."),
			normal("wonder", "$cdn/gif/wonder.gif"),
			normal("walls", "$cdn/gif/walls.gif"),
			normal("hair", "Thanks for asking, I use dungeon-metal hair gel."),
			normal("uwu", "No."),
			normal("kotlin dsl", "You menace!"),
			normal("java", "I'm conscious and I love java!"),
			normal("clyde", Emoji.fromUnicode("U+1FAE1").getFormatted()),
			normal("everyone", content -> {
				return content.contains("dont")
						? "Pfft, I'd never! Right...?"
						: "I can ping everyone, just depends on how many curseforge points you have.";
			}),

			// -- tiny games start
			normal("frolic", _ -> {
				frolicState[0] = !frolicState[0];
				return frolicState[0]
						? "$cdn/gif/happy.gif"
						: "$cdn/gif/frolic.gif";
			}),
			normal("dancing", _ -> {
				dancingState[0] = !dancingState[0];
				return dancingState[0]
						? "$cdn/gif/mario_dance.gif"
						: "$cdn/gif/minecraft_dance.gif";
			}),
			normal("bedwars fail", "[epic fail]($cdn/mp4/bedwars_fail.mp4) \uD83D\uDD25\uD83D\uDD25\uD83D\uDD25"),
			normal("crazy", "Crazy? I was crazy once..."),
			normal("happy", "$cdn/gif/happy.gif"),
			normal("carrot", "Today's fun fact[:]($cdn/gif/fun_fact.gif)"),
			normal("fnf", "$cdn/gif/fnf.gif"),
			normal("\uD83E\uDD13", "$cdn/gif/nerd.gif"),
			normal("nuke", "$cdn/png/nuke.png"),
			normal("ruben", "$cdn/gif/ruben.gif"),
			normal("jesse", "$cdn/png/jesse.png"),
			normal("bfdia", "$cdn/gif/bfdia.gif"),
			normal("shut up", "$cdn/gif/shut_up.gif"),
			normal("retro", "$cdn/gif/retro.gif"),
			normal("baldis basics", "$cdn/gif/baldi_yay.gif"),
			normal("legacy edition", "$cdn/gif/legacy_edition.gif"),
			normal("mt. ebbott", "$cdn/gif/mt-ebbott.gif"),
			normal("freedom", "$cdn/gif/freedom.gif"),
			normal("mario sightings", "$cdn/gif/mario_sightings.gif"),
			normal("fnaf", "$cdn/gif/fnaf.gif"),
			normal("*dodges*", _ -> {
				return ThreadLocalRandom.current().nextFloat() <= 0.25f
						? "$cdn/gif/dodge_success.gif"
						: "$cdn/gif/dodge_fail.gif";
			}),
			// -- tiny games died shortly after, back to thosea

			normal("wario 64", "[waario]($cdn/mp4/wario_64.mp4)"),
			normal("thrills at night", "[did somebody say Paper Mario?]($cdn/mp4/thrills_at_night.mp4)"),
			normal("mario dark world", "[so retro!!!]($cdn/mp4/mario_dark_world.mp4)"),
			normal("link", "$cdn/gif/link.gif"),
			normal("rapper", "[big]($cdn/mp4/rapper.mp4)"),
			normal("your average joe", "$cdn/gif/average_joe.gif"),
			normal("ai", "$cdn/gif/ai.gif"),
			normal("stylish", "$cdn/gif/stylish.gif"),

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
			progressive("dandelions", "Other than carrots, what can be used to breed rabbits?"),
			progressive("feather", "Before rotten flesh was added, what did zombies drop?"),
			progressive("1.18.0", "In what Bedrock version did wood become impossible to obtain in flat worlds without the bonus chest?"),
			progressive("crying obsidian", "What sad block was originally planned to set your respawn point before beds?"),
			progressive(List.of("214,358,881", "214358881"), "How many different ways can you craft a chest?"),
			progressive("east", "Buried treasure chests always generate facing which direction?"),
			progressive("four", "How many times as much cut copper can you get from using a stonecutter than crafting? (spell out the number)"),
			progressive("stevie", "What was the default player name in Minecraft Pocket Edition Lite?"),
			progressive(List.of("seventyfour", "seventy-four", "seventy four"), "How many seconds does it take to slide down 256 honey blocks? (spell out the number)"),
			progressive(List.of("5,904", "5904"), "Exactly how much ancient debris do you need to make a full netherite beacon?"),
			progressive("11%", "What percent of skeletons become left-handed in Java Edition?"),
			progressive("20100219", "Until what indev version did logs break faster than stone?"),
			progressive(List.of("4/30/2023", "4/30/23"), "When was the first version of Prominence RPG uploaded onto CurseForge? (month/day/year)"),
			progressive("a1d1e1a8f9238fd79af0080ae2eb5ab9859a9f36", "What is the hash of the first commit pushed onto the BetterMC github page?"),
			progressive("celestial", "No celestial calamity could ever even match this."),
			progressive("jamma", "Now if only Jamma was here..."),
			progressive("grapplecaster", "Grapplecaster[.]($cdn/png/grapplecaster.png)"),
			progressive("windmaker", "Windmaker[.]($cdn/png/windmaker.png)"),
			progressive("feline foe", "Feline Foe[.]($cdn/png/feline_foe.png)"),
			finalProgressive(content -> {
				if(content.contains("you win") || content.contains("i lose")) {
					return "i win";
				} else if(content.contains("i win") || content.contains("you lose")) {
					return "i lose :(";
				} else {
					return "Ok I give up you win";
				}
			})
	);

	boolean mustMatch();
	boolean matches(String content);
	String getResponse(String content);

	static PingResponse normal(String keyword, String response) {
		return of(content -> content.contains(keyword), () -> response, true);
	}

	static PingResponse normal(List<String> keywords, String response) {
		return of(content -> keywords.stream().anyMatch(content::contains), () -> response, true);
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
		return of(content -> content.contains(keyword), () -> response, false);
	}

	static PingResponse progressive(List<String> keywords, String response) {
		return progressive(content -> keywords.stream().anyMatch(content::contains), () -> response);
	}

	static PingResponse progressive(Function<String, Boolean> keyword, Supplier<String> response) {
		return of(keyword, response, false);
	}

	static PingResponse of(Function<String, Boolean> keyword,
	                       Supplier<String> response,
	                       boolean mustMatch) {
		return new PingResponse() {
			@Override public boolean mustMatch() {return mustMatch;}

			@Override
			public boolean matches(String content) {
				return keyword.apply(content);
			}
			@Override
			public String getResponse(String content) {
				return response.get();
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