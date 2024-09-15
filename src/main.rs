use anyhow::Result;
use dev_dungeon_bot::config::init_config;
use tracing::level_filters::LevelFilter;
use tracing_subscriber::{fmt, layer::SubscriberExt, util::SubscriberInitExt, EnvFilter};

#[tokio::main]
pub async fn main() -> Result<()> {
    dotenvy::dotenv()?;

    tracing_subscriber::registry()
        .with(
            EnvFilter::from_default_env()
                .add_directive(LevelFilter::INFO.into())
        )
        .with(
            fmt::layer()
                .pretty()
                .compact()
                .with_ansi(true)
                .with_level(true)
                .with_target(true)
                .with_file(false)
                .without_time(),
        )
        .init();

    let config = init_config()?;

    println!("{:#?}", config);

    Ok(())
}
