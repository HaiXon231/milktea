# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working on this project.

## Project Overview

AI Telegram bot cho quán trà sữa Casso. Khách chat → AI hiểu → gợi ý → đặt món → thanh toán QR payOS.

**Yêu cầu đầy đủ:** xem `Casso Entry Test - Intern Software Engineer.pdf`

**Runtime:** Java 21 + Spring Boot 3.5.5
**AI:** Groq API (`llama-3.3-70b-versatile`, temperature 0.1) — bypass Spring AI
**Database:** PostgreSQL via Spring Data JPA + Flyway migrations
**Payment:** payOS SDK v2.0.1 (QR code checkout)
**Bot:** java-telegram-bot-api v9.6.0 (Long Polling)

## Build & Run

```bash
# Build
mvn clean package -DskipTests

# Run locally
mvn spring-boot:run

# Run compiled JAR
java -jar target/milktea-bot-0.0.1-SNAPSHOT.jar
```

## Required Environment Variables

| Variable | Description |
|----------|-------------|
| `OPENAI_API_KEY` | Groq API key |
| `TELEGRAM_BOT_TOKEN` | Telegram bot token |
| `TELEGRAM_BOT_USERNAME` | Bot username |
| `PAYOS_CLIENT_ID` | payOS Client ID |
| `PAYOS_API_KEY` | payOS API Key |
| `PAYOS_CHECKSUM_KEY` | payOS Checksum Key |
| `APP_BASE_URL` | Public URL (default: http://localhost:8080) |
| `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` | PostgreSQL |

## Architecture

```
Customer Telegram → TeaShopBot (Long Polling)
                         ↓
                  CustomerService (get/create customer, save history)
                         ↓
                  GroqService (direct HTTP → Groq API, bypass Spring AI)
                  └── 9 tools: find_item_by_name, get_menu, view_cart,
                       add_to_cart, remove_from_cart, update_cart_item,
                       get_best_sellers, checkout, recommend
                         ↓
                  AiToolFunctions → MenuService / CartService / OrderService
                         ↓
                  PostgreSQL + payOS (QR payment + webhook)
```

## AI Design — CRITICAL RULES

### Tool Flow (BẮT BUỘC theo thứ tự):
1. Khách muốn đặt món → `find_item_by_name(name)` — tìm bằng tên tiếng Việt
2. Kết quả `FOUND:itemId|name|priceM|priceL` → **HỎI XÁC NHẬN**: "Trà sữa Socola, size M, 1 ly như vậy nhe con?"
3. Khách confirm → `add_to_cart(itemId, size, qty)`

**Sai:** Tìm xong gọi add_to_cart ngay → không xác nhận
**Đúng:** find_item_by_name → hỏi confirm → add_to_cart

### GroqService.java:
- Bypasses Spring AI completely (Spring AI swallows tool results with Groq)
- Direct `RestTemplate` HTTP → Groq API
- 2-turn tool loop: message+tools → Groq → execute → result → Groq → natural response
- Temperature: 0.1 (not 0.0 — reduces tool_call instability)
- Handles `"null"` literal string from Groq → empty object gracefully

### System Prompt Key Rules:
- Khi `find_item_by_name` trả `FOUND:` → KHÔNG in lại menu, CHỈ hỏi xác nhận
- Khi `get_menu` → IN ĐẦY ĐỦ menu
- TRƯỚC KHI `add_to_cart` → phải hỏi xác nhận khách
- TRƯỚC KHI `checkout` → thu thập tên + SDT + địa chỉ giao hàng

## Checkout Flow (payOS)

1. Customer: "thanh toán" → AI hỏi tên, SDT, địa chỉ
2. Đủ thông tin → `checkout(name, phone, address, note)`
3. Tạo `Order` (snapshot giỏ hàng) + payOS QR payment link
4. Customer quét QR → payOS webhook `/api/webhook/payos`
5. `PaymentWebhookController` verify HMAC → `OrderService.confirmPayment()`
6. `TeaShopBot.notifyPaymentSuccess()` gửi Telegram notification

## Database Migrations

| Migration | Description |
|-----------|-------------|
| V1__init_schema.sql | Tables: menu_item, customer, cart_item, orders, order_item, conversation_message |
| V2__seed_menu_data.sql | Seed menu từ Menu.csv |
| V3__add_sales_count.sql | Thêm cột sales_count cho best seller |
| V4__add_delivery_info.sql | Thêm delivery_name, delivery_phone, delivery_address |

## Known Bugs Fixed

| Bug | Symptom | Fix |
|-----|---------|-----|
| Groq trả `"arguments": "null"` | Bot crash → "Mẹ đang bận" | parseArgs() handle null literal |
| add_to_cart crash khi không có item_id | Crash | null-safety trong executeTool |
| Groq rate limit → "Mẹ đang bận" | 429 error | callGroqWithRetry() 3 retries, backoff 1.5s |
| Greeting gọi Groq (thất bại) | Greeting trả "Mẹ đang bận" | Fast path isGreeting() → buildGreeting() |

## Security Notes

- API keys in `.env` (not hardcoded) ✅
- HMAC-SHA256 payOS webhook verification ✅
- No SQL injection (JPA parameterized queries) ✅
- No rate limiting on webhook ⚠️ tech debt

## Key Files

| File | Purpose |
|------|---------|
| `ai/GroqService.java` | Direct Groq HTTP, 9 tools, 2-turn loop |
| `ai/AiToolFunctions.java` | Tool implementations + fuzzy search |
| `bot/TeaShopBot.java` | Telegram Long Polling listener |
| `service/MenuService.java` | Menu queries + `findByName()` fuzzy search |
| `service/CartService.java` | Cart CRUD |
| `service/OrderService.java` | Checkout + delivery info + payOS |
| `controller/PaymentWebhookController.java` | payOS webhook |
| `config/PayOSConfig.java` | PayOS client bean |
| `config/SpringAiConfig.java` | RestTemplate bean (timeouts) |
