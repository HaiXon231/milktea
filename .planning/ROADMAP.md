# ROADMAP — Execution Phases

## Milestones

- ✅ **v1.0 MVP** — Phase 1 (shipped 2026-04-21)
- 🚧 **v1.1 Production Readiness** — Phases 2-5 (in progress)

## Phases

<details open>
<summary>✅ v1.0 MVP (Phase 1) — SHIPPED 2026-04-21</summary>

- [x] Phase 1: Keep cart intact until payment succeeds (1/1 plans) — completed 2026-04-21
  Goal: Prevent cart from being cleared prematurely during checkout. Only clear the cart when the payOS webhook confirms successful payment.
  Depends on: Core ordering flow
  Plans:
  - [x] Remove `clearCart` from `checkout` logic.
  - [x] Move `clearCart` to payment webhook confirmation flow.
  - [x] Ensure cart mapping matches the customer ID associated with the paid order.

</details>

### 🚧 v1.1 Production Readiness (In Progress)

- [ ] Phase 2: Staff Notification (1 plans)
  Goal: Notify staff of successful payments.
  Depends on: Phase 1
- [ ] Phase 3: Persistent User Profile (1 plans)
  Goal: Store customer delivery data to prevent re-asking.
  Depends on: Phase 2
- [ ] Phase 4: Order Tracking & Re-order (1 plans)
  Goal: Allow customers to check order status and re-order previous items.
  Depends on: Phase 3
- [ ] Phase 5: Smart Recommendation (1 plans)
  Goal: Implement intelligent drink recommendations based on order history.
  Depends on: Phase 4

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
| --- | --- | --- | --- | --- |
| 1. Keep cart intact | v1.0 | 1/1 | Complete | 2026-04-21 |
| 2. Staff Notification | v1.1 | 0/1 | Not started | - |
| 3. Persistent User Profile | v1.1 | 0/1 | Not started | - |
| 4. Order Tracking & Re-order | v1.1 | 0/1 | Not started | - |
| 5. Smart Recommendation | v1.1 | 0/1 | Not started | - |
