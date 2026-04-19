# Casso Milktea Bot - AI Telegram Bot

## Build & Run
```bash
mvn clean package -DskipTests
java -jar target/milktea-bot-0.0.1-SNAPSHOT.jar
```

## Environment Variables
| Variable | Description |
|----------|-------------|
| OPENAI_API_KEY | OpenAI API key |
| TELEGRAM_BOT_TOKEN | Telegram Bot token from @BotFather |
| TELEGRAM_BOT_USERNAME | Telegram Bot username |
| PAYOS_CLIENT_ID | payOS Client ID |
| PAYOS_API_KEY | payOS API Key |
| PAYOS_CHECKSUM_KEY | payOS Checksum Key |
| APP_BASE_URL | Public URL for webhooks |
