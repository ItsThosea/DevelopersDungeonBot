package me.thosea.developersdungeon.util;

import me.thosea.developersdungeon.Main;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;

import java.util.EnumSet;
import java.util.List;

/**
 * some APIs require a user only for the ID<br>
 * use with caution!!
 */
public class UserMock implements User {
	private final long id;

	public UserMock(long id) {
		this.id = id;
	}

	@Override public String getName() {return unsupported();}
	@Override public String getGlobalName() {return unsupported();}

	@Override public String getDiscriminator() {return unsupported();}
	@Override public String getAvatarId() {return unsupported();}

	@Override public CacheRestAction<Profile> retrieveProfile() {return unsupported();}
	@Override public String getAsTag() {return unsupported();}

	@Override
	public boolean hasPrivateChannel() {return false;}
	@Override
	public CacheRestAction<PrivateChannel> openPrivateChannel() {return unsupported();}
	@Override
	public List<Guild> getMutualGuilds() {return unsupported();}

	@Override public boolean isBot() {return false;}
	@Override public boolean isSystem() {return false;}

	@Override public JDA getJDA() {return Main.jda;}

	@Override public EnumSet<UserFlag> getFlags() {return unsupported();}
	@Override public int getFlagsRaw() {return unsupported();}
	@Override public String getDefaultAvatarId() {return unsupported();}

	@Override
	public long getIdLong() {
		return id;
	}
	@Override
	public String getAsMention() {
		return "<@" + id + ">";
	}

	private <T> T unsupported() {
		throw new UnsupportedOperationException();
	}
}