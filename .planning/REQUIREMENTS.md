# Milestone v1.1 Requirements

## 1. Staff Notification (NOTIF)
- [ ] **NOTIF-01**: Admin/Staff Telegram channel receives immediate notification when an order is paid.
- [ ] **NOTIF-02**: Notification message contains Order Code, Items, Customer Phone, and Delivery Address.

## 2. Persistent User Profile (PROF)
- [ ] **PROF-01**: System persists Customer's delivery Name, Phone, and Address to the database upon first valid checkout.
- [ ] **PROF-02**: System automatically pre-fills or asks confirmation ("Giao địa chỉ cũ hả con?") for returning customers.
- [ ] **PROF-03**: Customer can easily update their delivery info if they want to ship elsewhere.

## 3. Order Tracking & Re-order (ORDER)
- [ ] **ORDER-01**: AI has a `check_order_status` tool to report real-time order status to the customer.
- [ ] **ORDER-02**: AI has a `get_order_history` tool to retrieve past orders for a customer.
- [ ] **ORDER-03**: Customer can say "Cho con ly như hôm qua" and the bot successfully adds the same item to cart.

## 4. Smart Recommendation (REC)
- [ ] **REC-01**: The `recommend` tool uses the user's order history (if available) to suggest personalized drinks.
- [ ] **REC-02**: AI proactively suggests favorite sizes and toppings based on historical preferences.

---

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| NOTIF-01    | 2     | To Do  |
| NOTIF-02    | 2     | To Do  |
| PROF-01     | 3     | To Do  |
| PROF-02     | 3     | To Do  |
| PROF-03     | 3     | To Do  |
| ORDER-01    | 4     | To Do  |
| ORDER-02    | 4     | To Do  |
| ORDER-03    | 4     | To Do  |
| REC-01      | 5     | To Do  |
| REC-02      | 5     | To Do  |
