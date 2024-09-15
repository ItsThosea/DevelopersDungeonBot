use anyhow::Result;
use serenity::{
    all::{GatewayIntents, GuildChannel, PartialGuild, Role},
    Client,
};

use crate::{config::Config, handler::BotEventHandler, util::IntoResult};

pub struct BotState {
    pub client: Client,
    pub config: Config,
    pub guild: PartialGuild,
    pub roles: BotRoles,
    pub channels: BotChannels,
}

pub struct BotRoles {
    pub team_role_sandwich_top: Role,
    pub team_role_sandwich_bottom: Role,
}

pub struct BotChannels {
    pub general: GuildChannel,
    pub minor_log: GuildChannel,
    pub major_log: GuildChannel,
    pub channel_log: GuildChannel,
    pub counts: GuildChannel,
    pub voting: GuildChannel,
}

impl BotChannels {
    pub async fn fetch(config: &Config, client: &Client) -> Result<BotChannels> {
        let general = client
            .http
            .get_channel(config.channels.general.into())
            .await?
            .guild()
            .res()?;

        let minor_log = client
            .http
            .get_channel(config.channels.minor_log.into())
            .await?
            .guild()
            .res()?;

        let major_log = client
            .http
            .get_channel(config.channels.major_log.into())
            .await?
            .guild()
            .res()?;

        let channel_log = client
            .http
            .get_channel(config.channels.channel_log.into())
            .await?
            .guild()
            .res()?;

        let counts = client
            .http
            .get_channel(config.channels.counts.into())
            .await?
            .guild()
            .res()?;

        let voting = client
            .http
            .get_channel(config.counts.voting.channel.into())
            .await?
            .guild()
            .res()?;

        Ok(BotChannels {
            general,
            minor_log,
            major_log,
            channel_log,
            counts,
            voting,
        })
    }
}

impl BotRoles {
    pub async fn fetch(config: &Config, client: &Client) -> Result<BotRoles> {
        let roles = client.http.get_guild_roles(config.server_id.into()).await?;

        let team_role_sandwich_top = roles
            .iter()
            .find(|v| v.id.get() == config.roles.team_role_sandwich_top)
            .res()?
            .clone();

        let team_role_sandwich_bottom = roles
            .iter()
            .find(|v| v.id.get() == config.roles.team_role_sandwich_bottom)
            .res()?
            .clone();

        Ok(BotRoles {
            team_role_sandwich_top,
            team_role_sandwich_bottom,
        })
    }
}

impl BotState {
    pub async fn init(config: Config, token: impl AsRef<str>) -> Result<BotState> {
        let intents = GatewayIntents::GUILD_MESSAGES
            | GatewayIntents::MESSAGE_CONTENT
            | GatewayIntents::GUILD_MEMBERS;

        let mut client = Client::builder(token, intents)
            .event_handler(BotEventHandler)
            .await?;

        client.start().await?;

        let guild = client.http.get_guild(config.server_id.into()).await?;
        let roles = BotRoles::fetch(&config, &client).await?;
        let channels = BotChannels::fetch(&config, &client).await?;

        Ok(BotState {
            guild,
            client,
            config,
            roles,
            channels,
        })
    }
}
