# ROADMAP — Execution Phases

## Milestones

- ✅ **v1.0 MVP** — Phase 1 (shipped 2026-04-21)

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

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
| --- | --- | --- | --- | --- |
| 1. Keep cart intact | v1.0 | 1/1 | Complete | 2026-04-21 |
