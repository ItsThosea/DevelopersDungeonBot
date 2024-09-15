use serenity::{all::{Context, EventHandler, Message}, async_trait};

pub struct BotEventHandler;

#[async_trait]
impl EventHandler for BotEventHandler {
    async fn message(&self, _ctx: Context, _msg: Message) {}
}
