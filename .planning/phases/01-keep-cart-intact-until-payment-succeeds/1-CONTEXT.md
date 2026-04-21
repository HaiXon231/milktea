# Phase 1: Keep cart intact until payment succeeds - Context

**Gathered:** 2026-04-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Trì hoãn việc xóa giỏ hàng cho đến khi có xác nhận thanh toán từ payOS webhook.
Hỗ trợ việc chỉ xóa đúng những món đã thanh toán để tránh ảnh hưởng đến các thao tác của người dùng sau khi tạo đơn.

</domain>

<decisions>
## Implementation Decisions

### 1. Xử lý Edge Case thêm món
- Khi webhook báo thanh toán thành công, hệ thống **CHỈ xóa những món nằm trong Order tương ứng**.
- Những món mới thêm vào giỏ hàng (không nằm trong Order này) sẽ được giữ nguyên.

### 2. Trải nghiệm UX/Thông báo
- Bot sẽ nhắn kèm một câu thông báo rõ ràng cho người dùng khi tạo đơn: "Mẹ vẫn giữ giỏ hàng cho con nếu con chưa thanh toán nhé." (kèm theo link QR).

### 3. Xử lý khi hủy thanh toán
- Khi người dùng hủy thanh toán qua link (được dẫn về trang `return url`), trạng thái của đơn hàng phải chuyển sang **CANCELLED** thay vì giữ nguyên PENDING.

### the agent's Discretion
- Code flow cụ thể trong `OrderService` và `CartService` để đối chiếu từng món và xóa.
- Thay đổi return url logic của webhook controller để đánh dấu `CANCELLED`.

</decisions>

<canonical_refs>
## Canonical References
No external specs — requirements are fully captured in decisions above
</canonical_refs>

<specifics>
## Specific Ideas
- Đảm bảo webhook xử lý chính xác logic xóa từng item để người dùng có thể thoái mái thêm món khác trong lúc chờ.
</specifics>

<deferred>
## Deferred Ideas
None — discussion stayed within phase scope
</deferred>

---

*Phase: 01-keep-cart-intact-until-payment-succeeds*
*Context gathered: 2026-04-21*
