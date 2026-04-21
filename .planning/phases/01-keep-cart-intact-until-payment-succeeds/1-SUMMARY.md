# Phase 1: Keep cart intact until payment succeeds - Execution Summary

**Completed:** 2026-04-21

<one_liner>
Đã cập nhật logic dọn giỏ hàng để chỉ xóa các món đã thanh toán, đồng thời cải tiến quy trình hỏi size/số lượng của AI và nhận diện chuẩn xác lệnh xóa toàn bộ giỏ hàng.
</one_liner>

<completed_steps>
## Completed Work
1. **Refactor Cart Clearing Logic:**
   - Xóa `clearCart` khỏi `OrderService.checkout()`.
   - Bổ sung `CartService.removeOrderItems(Customer, Order)` và gọi trong `confirmPayment` để hỗ trợ xóa riêng rẽ các món đã thanh toán (giữ các món mới thêm).
   - `PaymentRedirectController` nay gọi `cancelPayment` khi người dùng nhấn Hủy trên webhook url.
2. **Dynamic Size & Quantity Extraction:**
   - Thêm param `size` và `quantity` vào công cụ `find_item_by_name`.
   - Cập nhật logic `GroqService.executeTool` chặn việc thêm giỏ ngay nếu thiếu tham số, thay vào đó ghi đè pending state và yêu cầu AI hỏi khách hàng để làm rõ yêu cầu.
3. **Flexible Intent Recognition:**
   - Hoàn thiện `isClearCartRequest` để nhận biết câu lệnh tự nhiên (VD: "xóa toàn bộ tất cả các món đang trong giỏ").
   - Ưu tiên kiểm tra `CLEAR CART` trước `REMOVE SPECIFIC ITEM` để ngăn chặn nhận diện nhầm.
</completed_steps>

<verification>
## Verification
- Toàn bộ 41/41 unit tests trong dự án (bao gồm `AiTest` và logic database) đã chạy thành công (Exit Code 0).
- Hệ thống không bị vỡ chức năng liên quan đến Groq LLM hoặc Fast-path Intent Detector.
</verification>

<learnings>
## Technical Learnings
- Việc thêm item vào giỏ hàng ngay trong tool `find_item_by_name` (để tránh việc LLM bỏ sót bước) có thể gây ra hiện tượng tự ý áp đặt thông số (size, quantity) nếu không thiết kế cẩn thận. Yêu cầu AI đưa ra câu hỏi xác nhận kết hợp state memory (pending action) là cách giải quyết an toàn nhất.
</learnings>
