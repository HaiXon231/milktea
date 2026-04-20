package com.casso.milktea.ai;

import com.casso.milktea.model.Customer;
import com.casso.milktea.model.MenuItem;
import com.casso.milktea.service.CartService;
import com.casso.milktea.service.MenuService;
import com.casso.milktea.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * AI-callable tool implementations.
 *
 * Tool design philosophy:
 * - Each method catches its own exceptions → returns descriptive error string
 * - Tool results are fed back to Groq → Groq generates natural response
 * - All names/item_ids are resolved via Vietnamese fuzzy search
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiToolFunctions {

    private final MenuService menuService;
    private final CartService cartService;
    private final OrderService orderService;

    // ── 1. Tìm món theo tên tiếng Việt (thay vì item_id) ──────────

    /**
     * Tìm món trong menu bằng tên tiếng Việt (fuzzy search).
     * Dùng khi khách nói "thêm trà sữa socola" thay vì mã "TS01".
     *
     * @param query Tên món hoặc mô tả (VD: "trà sữa socola", "sữa tươi")
     * @return Kết quả tìm kiếm với item_id + tên + giá, hoặc lỗi nếu không tìm thấy
     */
    public String findItemByName(String query) {
        try {
            if (query == null || query.isBlank()) {
                return "Lỗi: bạn chưa cho biết tên món nha con!";
            }

            Optional<MenuItem> found = menuService.findByName(query);
            if (found.isEmpty()) {
                return "Lỗi: không tìm thấy món nào giống '" + query.trim()
                        + "'. Con nhắn lại tên món giúp mẹ nha!";
            }

            MenuItem item = found.get();
            return String.format("FOUND:%s|%s|%d|%d",
                    item.getItemId(),
                    item.getName(),
                    item.getPriceM(),
                    item.getPriceL());
        } catch (Exception e) {
            log.error("findItemByName failed: query={}", query, e);
            return "Lỗi khi tìm món: " + e.getMessage();
        }
    }

    // ── 2. Xem menu ────────────────────────────────────────────────

    public String getMenu(String category) {
        try {
            List<MenuItem> items = menuService.getMenu(category);
            if (items.isEmpty()) {
                return "Không có món nào"
                        + (category != null ? " trong danh mục " + category : "") + ".";
            }
            return menuService.formatMenuForAI(items);
        } catch (Exception e) {
            log.error("getMenu failed", e);
            return "Lỗi khi lấy menu: " + e.getMessage();
        }
    }

    // ── 3. Xem giỏ hàng ────────────────────────────────────────────

    public String viewCart(Customer customer) {
        try {
            return cartService.viewCart(customer);
        } catch (Exception e) {
            log.error("viewCart failed for customer={}", customer.getId(), e);
            return "Lỗi khi xem giỏ: " + e.getMessage();
        }
    }

    // ── 4. Thêm vào giỏ (sau khi khách xác nhận) ───────────────────

    /**
     * Thêm món vào giỏ. Item đã được xác định trước đó qua findItemByName.
     *
     * @param itemId  Mã món (VD: TS01) — resolved từ findItemByName
     * @param size    Size M hoặc L
     * @param quantity Số lượng
     */
    public String addToCart(Customer customer, String itemId, String size, int quantity) {
        try {
            return cartService.addToCart(customer, itemId, size, quantity);
        } catch (Exception e) {
            log.error("addToCart failed: customer={}, itemId={}", customer.getId(), itemId, e);
            return "Lỗi khi thêm món: " + e.getMessage();
        }
    }

    // ── 5. Xóa khỏi giỏ ─────────────────────────────────────────────

    public String removeFromCart(Customer customer, String itemId, String size) {
        try {
            return cartService.removeFromCart(customer, itemId, size);
        } catch (Exception e) {
            log.error("removeFromCart failed: customer={}, itemId={}", customer.getId(), itemId, e);
            return "Lỗi khi xóa món: " + e.getMessage();
        }
    }

    // ── 6. Sửa số lượng trong giỏ ──────────────────────────────────

    public String updateCartItem(Customer customer, String itemId, String size, int quantity) {
        try {
            return cartService.updateCartItem(customer, itemId, size, quantity);
        } catch (Exception e) {
            log.error("updateCartItem failed: customer={}, itemId={}", customer.getId(), itemId, e);
            return "Lỗi khi cập nhật giỏ: " + e.getMessage();
        }
    }

    // ── 7. Món bán chạy ───────────────────────────────────────────

    public String getBestSellers(String category) {
        try {
            var items = menuService.getBestSellers(category);
            if (items.isEmpty()) {
                return "Chưa có món bán chạy nào"
                        + (category != null ? " trong danh mục " + category : "") + ".";
            }
            return menuService.formatBestSellersForAI(items);
        } catch (Exception e) {
            log.error("getBestSellers failed", e);
            return "Lỗi khi lấy best seller: " + e.getMessage();
        }
    }

    // ── 8. Checkout (tạo đơn + link QR) ────────────────────────────

    /**
     * Chốt đơn + tạo link thanh toán payOS.
     * Gửi kèm thông tin giao hàng (name, phone, address) đã thu thập.
     */
    public OrderService.OrderResult checkout(Customer customer, String name,
            String phone, String address, String note) {
        try {
            // Validate delivery info
            StringBuilder missing = new StringBuilder();
            if (name == null || name.isBlank()) missing.append("tên, ");
            if (phone == null || phone.isBlank()) missing.append("số điện thoại, ");
            if (address == null || address.isBlank()) missing.append("địa chỉ giao hàng, ");
            if (missing.length() > 0) {
                String info = missing.toString();
                return new OrderService.OrderResult(false,
                        "Còn thiếu: " + info.substring(0, info.length() - 2)
                                + ". Con nhắn đủ thông tin giúp mẹ nha!",
                        null, null);
            }

            return orderService.checkout(customer, name, phone, address, note);
        } catch (Exception e) {
            log.error("checkout failed for customer={}", customer.getId(), e);
            return new OrderService.OrderResult(false,
                    "Lỗi khi tạo đơn: " + e.getMessage(), null, null);
        }
    }

    // ── 9. Đề xuất (dựa trên sở thích) ─────────────────────────────

    /**
     * Gợi ý đồ uống theo sở thích.
     * AI gọi tool này khi khách nói VD: "mình thích socola", "cho mình gợi ý đi"
     */
    public String getRecommendation(String preference) {
        try {
            if (preference == null || preference.isBlank()) {
                // No preference → return best sellers
                return menuService.formatBestSellersForAI(menuService.getBestSellers(null));
            }

            String p = preference.toLowerCase();
            List<MenuItem> all = menuService.getMenu(null);

            // Match keywords to categories or item names
            java.util.List<MenuItem> matches = new java.util.ArrayList<>();
            for (MenuItem item : all) {
                String name = item.getName().toLowerCase();
                String cat = item.getCategory().toLowerCase();
                if (cat.contains(p) || name.contains(p)) {
                    matches.add(item);
                }
            }

            if (matches.isEmpty()) {
                return "Mẹ chưa hiểu ý con lắm 😅 Con mô tả thêm được không? "
                        + "Ví dụ: socola, trà xanh, matcha, sữa tươi...";
            }

            // Return top 3 matches
            StringBuilder sb = new StringBuilder("Mẹ gợi ý cho con nè:\n\n");
            for (int i = 0; i < Math.min(3, matches.size()); i++) {
                MenuItem item = matches.get(i);
                sb.append(String.format("• %s — M: %,dđ / L: %,dđ\n",
                        item.getName(), item.getPriceM(), item.getPriceL()));
            }
            sb.append("\nCon thích món nào thì nhắn mẹ nha!");
            return sb.toString();
        } catch (Exception e) {
            log.error("getRecommendation failed: preference={}", preference, e);
            return "Lỗi khi gợi ý: " + e.getMessage();
        }
    }
}