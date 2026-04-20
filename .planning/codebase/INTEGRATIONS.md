# INTEGRATIONS — External Services & APIs

**Date:** 2026-04-20  
**Focus:** tech

## Groq API (AI)

- **Purpose:** Conversational AI via Spring AI ChatClient (`RestTemplate` direct HTTP calls)
- **Model:** `llama-3.3-70b-versatile` with function calling
- **Base URL:** `https://api.groq.com/openai/v1/chat/completions`
- **Auth:** Bearer token via `OPENAI_API_KEY`
- **Files:** `GroqService.java`, `AiChatService.java`, `AiToolFunctions.java`
- **Flow:** 2-turn loop — send message + tools → Groq → execute tool → send result → Groq → natural response

## Telegram Bot

- **Library:** `java-telegram-bot-api` v9.6.0
- **Mechanism:** Long Polling (not Webhook)
- **Bean:** `TelegramBot` in `TelegramBotConfig.java`
- **Entry point:** `TeaShopBot.java` — `UpdatesListener` loop
- **Config:** `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`

## payOS (Payment)

- **SDK:** `payos-java` v2.0.1
- **Bean:** `PayOS` in `PayOSConfig.java`
- **APIs used:**
  - `paymentRequests().create()` — create payment link with QR
  - `webhooks().verify()` — verify HMAC-SHA256 webhook signature
- **Webhook endpoint:** `POST /api/webhook/payos` → `PaymentWebhookController`
- **Success redirect:** `GET /payment/success`
- **Cancel redirect:** `GET /payment/cancel`

## PostgreSQL (Database)

- **ORM:** Spring Data JPA + Hibernate
- **Migration:** Flyway (version-controlled SQL migrations)
- **Connection:** via environment variables (`PGHOST`, `PGPORT`, etc.)
- **DDL mode:** `validate` (schema must match migrations, not auto-generated)

## Migration Files

| File | Description |
|------|-------------|
| `V1__init_schema.sql` | Tables: `menu_item`, `customer`, `cart_item`, `orders`, `order_item`, `conversation_message` |
| `V2__seed_menu_data.sql` | Seed menu items from `Menu.csv` |
| `V3__add_sales_count.sql` | Add `sales_count` column to `menu_item` |

## External Services Summary

```
Telegram User → Telegram Long Polling API
                    ↓
              Spring Boot App
                    ↓
         ┌──────────┼──────────┐
         ↓          ↓          ↓
    Groq API   payOS API  PostgreSQL
   (AI chat)  (payments)  (data)
```
