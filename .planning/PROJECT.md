# PROJECT — Casso Milktea Bot

## What This Is
An AI-powered Telegram chatbot for the "Casso" milk tea shop. The bot acts as a friendly Vietnamese mother to manage communication, ordering, and payments (via payOS).

## Why It Exists
To automate order taking and payment processing while providing a warm, conversational, and natural user experience through AI.

## Core Value
A seamless conversational checkout experience that feels like chatting with a real person (a motherly figure), backed by a robust order management and payment integration system.

## Requirements

### Validated
- ✓ Telegram Long Polling integration — v1.0
- ✓ Conversational AI interface via Groq API (llama-3.3-70b-versatile) — v1.0
- ✓ Intent detection (Fast-path and LLM reasoning) — v1.0
- ✓ Menu and Cart management — v1.0
- ✓ Order tracking (AWAITING_PAYMENT, PAID, COMPLETED, CANCELLED) — v1.0
- ✓ QR code payment integration via payOS API — v1.0
- ✓ Keep cart intact until payment succeeds (delay clearCart until payOS webhook confirmation) — v1.0

### Active
- [ ] Tích hợp thông báo qua Telegram cho Staff/Admin (Staff Notification)
- [ ] Lưu trữ thông tin giao hàng mặc định của khách (Persistent User Profile)
- [ ] Khả năng tra cứu tình trạng đơn hàng và đặt lại món cũ (Order Tracking & Re-order)
- [ ] Gợi ý món thông minh dựa trên lịch sử mua (Smart Recommendation)

### Out of Scope
- [None yet]

## Key Decisions
| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Use Groq direct HTTP API | Overcome Spring AI function calling instability | Working |
| Builder pattern | Replace Lombok for reliable bytecode generation | Working |
| Delay cart clearance | Ensure users do not lose their cart if payment is cancelled or fails | Working |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-21 after starting v1.1*
