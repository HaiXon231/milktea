# ARCHITECTURE — System Design & Patterns

**Date:** 2026-04-20  
**Focus:** arch

## Architecture Pattern

**Layered Service Architecture** with AI function-calling orchestration:

```
Telegram Long Polling
        ↓
TeaShopBot (listener + sender)
        ↓
CustomerService (get/create customer, save history)
        ↓
GroqService (direct HTTP → Groq API with 2-turn tool loop)
        ↓
AiToolFunctions (delegate to services)
        ↓
    ┌────┴────┬───────────┬───────────┐
    ↓         ↓           ↓           ↓
MenuService CartService OrderService
    ↓         ↓           ↓           ↓
PostgreSQL                   payOS API
                         (payment link + webhook)
```

## Request Flow (Single Message)

1. `TeaShopBot.handleUpdate()` receives Telegram text message
2. `CustomerService.getOrCreateCustomer()` — auto-register or retrieve customer
3. `AiChatService.chat()` → `GroqService.chat()`:
   - Turn 1: Send (system + history + user msg + 7 tool defs) → Groq
   - If Groq calls a tool → execute in Java → send result back to Groq
   - Turn 2: Groq returns natural-language response
4. Response sent back to customer via Telegram
5. Payment webhook (`/api/webhook/payos`) → confirm order → notify customer

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Groq direct HTTP (not Spring AI wrapper) | Spring AI function-calling loop broken with Groq; bypass gives full control |
| `GroqService` separate from `AiChatService` | `AiChatService` is facade; `GroqService` handles raw API |
| Lombok removed from models | Lombok annotation processor not generating bytecode reliably at runtime |
| Builder pattern on models | Replaces Lombok `@Builder`; chainable construction |
| `sales_count` on `menu_item` | Tracks best seller data for AI recommendation feature |

## Anti-Hallucination Strategy (3 Layers)

1. **System prompt rules** — explicit: only sell from menu, never invent prices
2. **Function schema** — `itemId` validated, `size` enum M/L
3. **Backend validation** — every cart operation checks `itemId` exists + `available=true`

## Order Lifecycle

```
PENDING → AWAITING_PAYMENT → PAID → PREPARING → COMPLETED / CANCELLED
```

- `AWAITING_PAYMENT`: after `checkout()` call, before payOS confirms
- `PAID`: after payOS webhook HMAC verified
- `order_item` is a **snapshot** (price frozen at checkout, not FK to menu)

## Conversation History

- Stored in `conversation_message` table
- Loaded in reverse order (newest first) then reversed to chronological
- Limited to **8 messages** (to avoid token overflow on Groq)
