# CONCERNS — Tech Debt, Issues & Fragile Areas

**Date:** 2026-04-20  
**Focus:** concerns

## Known Issues

### 1. Spring AI Function Calling Broken with Groq (RESOLVED)
- **Severity:** Critical
- **Problem:** Spring AI's `ChatClient` tool callback loop did not correctly handle Groq's `tool_calls` response format → function results were swallowed → bot replied with "Con xem menu nay..." instead of actual data
- **Resolution:** Bypassed Spring AI entirely; `GroqService.java` calls Groq API directly via `RestTemplate` with manual 2-turn loop
- **Files affected:** `ai/GroqService.java` (new), `ai/AiChatService.java` (now facade only)

### 2. Lombok Not Generating Bytecode at Runtime (RESOLVED)
- **Severity:** Critical
- **Problem:** `NoClassDefFoundError: Customer` at runtime — Lombok annotation processor not running correctly in some environments
- **Resolution:** Removed all Lombok annotations from model classes; hand-wrote constructors, getters, setters, and builder patterns
- **Files affected:** All model classes (`Customer`, `ConversationMessage`, `CartItem`, `MenuItem`, `Order`, `OrderItem`)

### 3. Telegram Bot Deprecated API Warning
- **Severity:** Low
- **Problem:** `TeaShopBot.java` uses deprecated `UpdatesListener` constructor
- **Note:** `java-telegram-bot-api` v9.6.0 — update to latest if available

### 4. Groq Rate Limits (Known Limitation)
- **Severity:** Medium
- **Problem:** Groq free tier has aggressive rate limits; "Mẹ đang bận" message shown on 429 errors
- **Mitigation:** Exponential backoff not yet implemented; retry logic is simple catch-and-reply

## Technical Debt

| Item | Description | Priority |
|------|-------------|----------|
| No unit tests | Only `AiTest.java` placeholder exists | High |
| `sales_count` all zeros | Best seller feature works but data is uninitialized | Medium |
| No rate limiting on webhook | `PaymentWebhookController` trusts payOS HMAC only | Medium |
| No idempotency guard | Duplicate payOS webhooks processed twice | Low |
| No conversation message cleanup | History grows unbounded | Low |

## Security Notes

| Item | Status |
|------|--------|
| API keys in `.env` (not hardcoded) | ✅ Good |
| HMAC-SHA256 payOS webhook verification | ✅ Good |
| No SQL injection (JPA parameterized queries) | ✅ Good |
| Telegram token in env var | ✅ Good |
| Rate limiting on webhook | ❌ Missing |

## Performance Considerations

| Area | Status |
|------|--------|
| Lazy loading on `@ManyToOne` | ✅ Applied |
| History limited to 8 messages | ✅ Prevents token overflow |
| `salesCount` indexed via default PK | ✅ Sufficient for scale |
| Long Polling on Telegram (not Webhook) | ✅ Simple, no tunnel needed |

## Fragile Areas

- **`GroqService.callGroq()`** — if Groq API changes response structure, `extractText()` and tool call parsing will break
- **`ConversationMessageRepository.findTopNByCustomerIdOrderByCreatedAtDesc()`** — uses `Pageable.ofSize()`; ensure JPA query correctly handles `Pageable`
- **payOS webhook** — HMAC verification is the only auth; if `CHECKSUM_KEY` is wrong, all webhooks are rejected silently
