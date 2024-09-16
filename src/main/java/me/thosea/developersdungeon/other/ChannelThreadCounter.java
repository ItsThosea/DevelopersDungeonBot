package me.thosea.developersdungeon.other;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.Constants;
import me.thosea.developersdungeon.util.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel.AutoArchiveDuration;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Iterator;

public class ChannelThreadCounter {
	private static final Long2ObjectOpenHashMap<ChannelThreadCounter> COUNTERS = new Long2ObjectOpenHashMap<>();
	private static final int STACK_SIZE_LIMIT = 25;

	private final String namePrefix;
	private final boolean showAuthorName;
	private final Message countMessage;

	private final Path lastThreadedMessagesJson;

	private final ArrayDeque<ThreadEntry> lastThreadedMessages = new ArrayDeque<>(STACK_SIZE_LIMIT);
	private int count = 0;

	public ChannelThreadCounter(String id, String namePrefix, boolean showAuthorName) {
		this.namePrefix = namePrefix + " #";
		this.showAuthorName = showAuthorName;
		this.lastThreadedMessagesJson = Paths.get("./devdungeon_autothread_" + id + ".json");

		long countMessageId = Constants.id("counts." + id + "_message");
		long sourceChannel = Constants.id("counts." + id + "_channel");

		if(Main.countsChannel != null && countMessageId > 1 && sourceChannel > 1) {
			COUNTERS.put(sourceChannel, this);
			this.countMessage = getCountMessage(Main.countsChannel, id, countMessageId);

			TextChannel channel = Main.guild.getTextChannelById(sourceChannel);

			if(channel != null && Files.exists(lastThreadedMessagesJson)) {
				try {
					JsonArray array = Utils.GSON.fromJson(Files.readString(lastThreadedMessagesJson), JsonArray.class);
					synchronized(lastThreadedMessages) {
						for(int i = 0; i < array.size(); i++) {
							ThreadEntry entry = ThreadEntry.read(array.get(i).getAsJsonObject());
							lastThreadedMessages.push(entry);

							channel.retrieveMessageById(entry.messageId).queue(
									_ -> {}, // message exists
									_ -> this.removeMessage(entry.messageId) // message is gone
							);
						}
					}
				} catch(Exception e) {
					System.err.println("Failed to read channel thread counter message stack from " + lastThreadedMessagesJson.toAbsolutePath());
					e.printStackTrace();
				}
			}
		} else {
			this.countMessage = null;
			if(Main.countsChannel != null) {
				System.out.println("Thread counter " + id + " is individually disabled.");
			}
		}
	}

	@Nullable
	private Message getCountMessage(TextChannel channel, String id, long countMessageId) {
		Message result;
		try {
			result = channel.retrieveMessageById(countMessageId).submit().get();
			if(!result.getAuthor().equals(Main.jda.getSelfUser())) {
				System.err.println("Warning: thread counter " + id + " has a message not owned by me.");
				System.err.println("Counts will not be persisted.");
				result = null;
			} else {
				try {
					this.count = Integer.parseInt(result.getContentRaw());
				} catch(NumberFormatException ignored) {
					System.err.println("Warning: thread counter " + id + " points to a non-number message.");
					System.err.println("Counts will not be persisted.");
					result = null;
				}
			}
		} catch(Exception e) {
			result = null;
			System.err.println("Warning: thread counter " + id + " isn't disabled but doesn't point to a valid message.");
			System.err.println("Counts will not be persisted.");
		}
		return result;
	}

	public void makeThread(Message msg) {
		this.setCount(count + 1);

		String nameSuffix;
		if(!showAuthorName) {
			nameSuffix = "";
		} else {
			if(!msg.getEmbeds().isEmpty() && msg.getEmbeds().getFirst().getAuthor() != null) {
				nameSuffix = " - " + msg.getEmbeds().getFirst().getAuthor().getName();
			} else {
				nameSuffix = " - " + msg.getAuthor().getName();
			}
		}

		msg.createThreadChannel(namePrefix + count + nameSuffix)
				.setAutoArchiveDuration(AutoArchiveDuration.TIME_24_HOURS)
				.queue(channel -> {
					synchronized(lastThreadedMessages) {
						if(lastThreadedMessages.size() == STACK_SIZE_LIMIT) {
							lastThreadedMessages.removeLast();
						}

						this.lastThreadedMessages.push(new ThreadEntry(msg.getIdLong(), channel.getIdLong(), count));
						this.writeLastThreadedMessages();
					}
				});
	}

	public void removeMessage(long messageId) {
		boolean foundMessage = false;

		synchronized(lastThreadedMessages) {
			Iterator<ThreadEntry> iterator = lastThreadedMessages.descendingIterator();

			while(iterator.hasNext()) {
				ThreadEntry entry = iterator.next();
				ThreadChannel thread = Main.guild.getThreadChannelById(entry.threadId);

				if(foundMessage) {
					if(thread != null) {
						var newName = thread.getName().replaceFirst("#" + entry.count, "#" + --entry.count);
						thread.getManager().setName(newName).queue();
					}
				} else if(entry.messageId == messageId) {
					this.setCount(count - 1);
					iterator.remove();
					foundMessage = true;
					if(thread != null) thread.delete().queue();
				}
			}
		}

		if(foundMessage) {
			writeLastThreadedMessages();
		}
	}

	private void writeLastThreadedMessages() {
		JsonArray array;
		synchronized(lastThreadedMessages) {
			array = new JsonArray(lastThreadedMessages.size());
			for(Iterator<ThreadEntry> it = lastThreadedMessages.descendingIterator(); it.hasNext(); ) {
				array.add(it.next().serialize());
			}
		}

		try {
			Files.writeString(lastThreadedMessagesJson, Utils.GSON.toJson(array));
		} catch(Exception e) {
			System.err.println("Failed to write channel thread counter message stack to " + lastThreadedMessagesJson.toAbsolutePath());
			e.printStackTrace();
		}
	}

	public void setCount(int count) {
		this.count = count;
		if(this.countMessage != null && !countMessage.getContentRaw().equals("" + count)) {
			this.countMessage.editMessage("" + count).queue();
		}
	}

	@Nullable
	public static ChannelThreadCounter getCounter(long channelId) {
		return COUNTERS.get(channelId);
	}

	@Nullable
	public static ChannelThreadCounter getCounterByCountMessage(long messageId) {
		return COUNTERS.values()
				.stream()
				.filter(counter -> counter.countMessage.getIdLong() == messageId)
				.findFirst()
				.orElse(null);
	}

	private static final class ThreadEntry {
		private final long messageId;
		private final long threadId;

		private int count;

		private ThreadEntry(long messageId, long threadId, int count) {
			this.messageId = messageId;
			this.threadId = threadId;
			this.count = count;
		}

		public static ThreadEntry read(JsonObject obj) {
			return new ThreadEntry(
					obj.get("message_id").getAsLong(),
					obj.get("thread_id").getAsLong(),
					obj.get("thread_count").getAsInt()
			);
		}

		public JsonObject serialize() {
			JsonObject obj = new JsonObject();
			obj.addProperty("message_id", messageId);
			obj.addProperty("thread_id", threadId);
			obj.addProperty("thread_count", count);
			return obj;
		}
	}
}