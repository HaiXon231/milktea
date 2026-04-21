# Phase 1: Keep cart intact until payment succeeds - Plan

**Status:** Approved
**Created:** 2026-04-21

<objective>
Ngăn chặn giỏ hàng bị xóa sớm trong lúc checkout, xử lý chính xác logic kích cỡ (size) & số lượng (quantity) khi thêm món, và hỗ trợ xóa toàn bộ giỏ hàng theo ý định tự nhiên của người dùng.
</objective>

<context>
- Context gathered from `1-CONTEXT.md`
- Codebase đã được kiểm tra (CartService, OrderService, GroqService, IntentDetector).
</context>

<steps>
## Implementation Steps

### 1. Refactor Cart Clearing Logic
- **`OrderService.java`**: Xóa `clearCart` khỏi phương thức `checkout`. Chuyển sang gọi trong `confirmPayment`.
- **`CartService.java`**: Thêm phương thức `removeOrderItems(Customer, Order)` để chỉ xóa đúng những món trong đơn hàng đã thanh toán, giữ lại các món người dùng mới thêm vào.
- **`PaymentRedirectController.java`**: Cập nhật hàm xử lý cancel để đánh dấu trạng thái đơn hàng thành `CANCELLED`.

### 2. Update AI Tool Definitions
- **`GroqService.java`**: Bổ sung `size` và `quantity` vào định nghĩa tool `find_item_by_name`.
- **`GroqService.java`**: Cập nhật `SYSTEM_PROMPT` bắt AI phải hỏi lại người dùng "Size M hay L, mấy ly?" nếu họ không cung cấp đủ.

### 3. Improve Intent Recognition
- **`IntentDetector.java`**: Nâng cấp hàm `isClearCartRequest` để nhận diện ngữ nghĩa lệnh xóa toàn bộ giỏ (VD: "xóa hết tất cả món đang trong giỏ hàng").
- Đổi thứ tự kiểm tra `CLEAR CART` lên trước `REMOVE SPECIFIC ITEM` để tránh xung đột từ khóa.
</steps>

<verification>
## Verification
- Toàn bộ thay đổi phải pass `mvn test`.
- Webhook trigger thử nghiệm với URL cancel phải báo Cancelled.
</verification>
