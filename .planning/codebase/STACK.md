# STACK — Technology & Dependencies

**Date:** 2026-04-20  
**Focus:** tech

## Runtime & Language

| Component | Version | Notes |
|-----------|---------|-------|
| Java | 21 | Virtual Threads, Records |
| Spring Boot | 3.5.5 | Auto-config, production-ready |
| Maven | — | Build tool |
| JRE | eclipse-temurin:21-jre-alpine | Multi-stage Dockerfile |

## Core Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `spring-boot-starter-web` | managed | REST controllers, web |
| `spring-boot-starter-data-jpa` | managed | ORM, repositories |
| `spring-boot-starter-validation` | managed | Input validation |
| `postgresql` | managed | Database driver |
| `flyway-core` | managed | DB migrations |
| `flyway-database-postgresql` | managed | PostgreSQL migration support |
| `spring-ai-openai-spring-boot-starter` | 1.1.4 | AI ChatClient (Groq API) |
| `java-telegram-bot-api` | 9.6.0 | Telegram Long Polling bot |
| `payos-java` | 2.0.1 | QR code payment integration |
| `lombok` | 1.18.38 | Boilerplate reduction (getters/setters) |
| `jackson-databind` | managed | JSON serialization |

## Spring AI Configuration

- **Model:** `llama-3.3-70b-versatile` (via Groq)
- **API URL:** `https://api.groq.com/openai`
- **Temperature:** 0.0
- **AI Mode:** `openai` (default) or `mock`

## Key Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven dependencies, Lombok processor config |
| `src/main/resources/application.yml` | All runtime config (DB, AI, Telegram, payOS) |
| `.vscode/launch.json` | Spring Boot run config with `.env` auto-load |
| `Dockerfile` | Multi-stage Docker build (JDK 21 → JRE 21) |

## Environment Variables

All sensitive config is externalized via env vars (defaults in `application.yml`):

```
OPENAI_API_KEY          # Groq API key
TELEGRAM_BOT_TOKEN      # Telegram bot token
TELEGRAM_BOT_USERNAME   # Bot username
PAYOS_CLIENT_ID         # payOS Client ID
PAYOS_API_KEY           # payOS API Key
PAYOS_CHECKSUM_KEY      # payOS HMAC key
APP_BASE_URL            # Public URL for webhooks
AI_MODE                 # openai or mock
PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD  # PostgreSQL
```
