---
created: 2026-04-21T08:10:00+07:00
title: Keep cart intact until payment succeeds
area: api/checkout
files:
  - src/main/java/com/casso/milktea/service/OrderService.java
  - src/main/java/com/casso/milktea/controller/PaymentWebhookController.java
---

## Problem

Khi bot gửi mã QR thanh toán (checkout), giỏ hàng hiện tại đang bị xóa ngay lập tức. Điều này gây bất tiện vì nếu người dùng đổi ý, bấm hủy thanh toán, hoặc thanh toán lỗi, toàn bộ giỏ hàng của họ sẽ biến mất và phải đặt lại từ đầu.

## Solution

Thay vì xóa giỏ hàng ngay lúc gọi hàm `checkout`, hãy trì hoãn việc xóa giỏ hàng (`clearCart`) cho đến khi hệ thống nhận được webhook xác nhận thanh toán thành công từ payOS. 

Các bước có thể:
1. Gỡ bỏ hàm xóa giỏ hàng bên trong luồng `OrderService.checkout`.
2. Chuyển logic xóa giỏ hàng sang `OrderService.confirmPayment` (hoặc `PaymentWebhookController`), hàm này được kích hoạt khi webhook trả về mã "00" (thanh toán thành công).
3. Đảm bảo giỏ hàng bị xóa chính xác dựa trên `customerId` liên kết với `orderCode`.
