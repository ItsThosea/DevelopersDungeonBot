use std::{fs, path::PathBuf};

use anyhow::Result;

#[derive(Debug, Clone, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize, Default)]
pub struct Config {
    pub server_id: u64,
    pub admins: Vec<u64>,
    pub channels: ChannelsConfig,
    pub categories: CategoriesConfig,
    pub counts: CountsConfig,
    pub content: ContentConfig,
    pub roles: RolesConfig,
    pub emojis: EmojisConfig,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize, Default)]
pub struct ChannelsConfig {
    pub general: u64,
    pub information: u64,
    pub verify: u64,
    pub suggestions: u64,
    pub announcements: u64,
    pub minor_log: u64,
    pub major_log: u64,
    pub channel_log: u64,
    pub bots: u64,
    pub commissions: u64,
    pub applications: u64,
    pub counts: u64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize, Default)]
pub struct CategoriesConfig {
    pub private_channels: u64,
    pub archived_channels: u64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize, Default)]
pub struct CountsConfig {
    pub suggestions: MessageAndChannel,
    pub modloader: MessageAndChannel,
    pub platform: MessageAndChannel,
    pub minecraft: MessageAndChannel,
    pub voting: MessageAndChannel,
    pub new_releases: MessageAndChannel,
    pub update_releases: MessageAndChannel,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize, Default)]
pub struct ContentConfig {
    // format to `<@&{}>`
    pub curseforge_ping_role_id: i64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize, Default)]
pub struct RolesConfig {
    pub team_role_sandwich_top: u64,
    pub team_role_sandwich_bottom: u64,
    pub staff: u64,
    pub verified: u64,
    pub content_creator: u64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize, Default)]
pub struct EmojisConfig {
    pub yeah: u64,
    pub keoiki: u64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize, Default)]
pub struct MessageAndChannel {
    pub message: u64,
    pub channel: u64,
}

pub fn init_config() -> Result<Config> {
    let path = PathBuf::from("config.toml");

    if !path.exists() {
        fs::write(&path, toml::to_string_pretty(&Config::default())?)?;
    }

    Ok(toml::from_str(&fs::read_to_string(path)?)?)
}
