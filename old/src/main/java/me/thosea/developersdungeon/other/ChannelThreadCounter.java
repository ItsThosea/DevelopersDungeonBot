package me.thosea.developersdungeon.other;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.thosea.developersdungeon.Main;
import me.thosea.developersdungeon.util.Constants;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel.AutoArchiveDuration;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Iterator;

public class ChannelThreadCounter {
	private static final Long2ObjectOpenHashMap<ChannelThreadCounter> COUNTERS = new Long2ObjectOpenHashMap<>();
	private static final int STACK_SIZE_LIMIT = 15;

	private final String namePrefix;
	private final boolean showAuthorName;
	private final Message countMessage;

	private final ArrayDeque<ThreadEntry> lastThreadedMessages = new ArrayDeque<>(STACK_SIZE_LIMIT);
	private int count = 0;

	public ChannelThreadCounter(String id, String namePrefix, boolean showAuthorName) {
		this.namePrefix = namePrefix + " #";
		this.showAuthorName = showAuthorName;

		long countMessageId = Constants.id("counts." + id + "_message");
		long sourceChannel = Constants.id("counts." + id + "_channel");

		if(Main.countsChannel != null && countMessageId > 1 && sourceChannel > 1) {
			COUNTERS.put(sourceChannel, this);
			this.countMessage = getCountMessage(Main.countsChannel, id, countMessageId);
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
					}
				});
	}

	public void removeMessage(long messageId) {
		synchronized(lastThreadedMessages) {
			Iterator<ThreadEntry> iterator = lastThreadedMessages.descendingIterator();

			boolean renameThreads = false;
			while(iterator.hasNext()) {
				ThreadEntry entry = iterator.next();
				ThreadChannel thread = Main.guild.getThreadChannelById(entry.threadId);

				if(renameThreads) {
					if(thread != null) {
						var newName = thread.getName().replaceFirst("#" + entry.count, "#" + --entry.count);
						thread.getManager().setName(newName).queue();
					}
				} else if(entry.messageId == messageId) {
					this.setCount(count - 1);
					iterator.remove();
					renameThreads = true;
					if(thread != null) thread.delete().queue();
				}
			}
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
	}
}