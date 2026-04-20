package com.casso.milktea.ai;

import com.casso.milktea.model.Customer;
import org.springframework.stereotype.Component;

/**
 * Fast-path intent detector — simple string matching, no regex, no Unicode issues.
 *
 * Runs BEFORE Gemini for high-confidence intents so that:
 * - "menu", "xem menu", "thực đơn" → get_menu() directly
 * - "giỏ hàng", "bill", "xem đơn" → view_cart() directly
 * - "thanh toán" → checkout flow directly
 *
 * No regex — plain contains() and equals() on lowercase strings.
 * This avoids Unicode/encoding bugs that plague regex on Windows.
 */
@Component
public class IntentDetector {

    /**
     * Detect intent from user message.
     * Returns a DetectedIntent for fast-path handling, or null for Gemini reasoning.
     */
    public DetectedIntent detect(String userMessage, Customer customer) {
        if (userMessage == null || userMessage.isBlank()) {
            return null;
        }

        String msg = userMessage.trim();
        String lower = msg.toLowerCase();

        // ── Exact / simple matches (no regex needed) ─────────────

        // GREETING
        if (isGreeting(lower)) {
            return DetectedIntent.greeting();
        }

        // HELP
        if (lower.equals("mẹ ơi") || lower.equals("mame") || lower.equals("help")
                || lower.equals("giúp mẹ") || lower.equals("bạn là ai")
                || lower.startsWith("bạn là") || lower.startsWith("mẹ là")) {
            return DetectedIntent.help();
        }

        // MENU — many ways to say it
        if (isMenuRequest(lower, msg)) {
            String category = extractCategoryAfterKeyword(lower, msg, "menu", "thực đơn", "danh sách món");
            return DetectedIntent.menu(category);
        }

        // VIEW CART — many ways to say it
        if (isCartViewRequest(lower, msg)) {
            return DetectedIntent.viewCart();
        }

        // CLEAR CART
        if (lower.equals("xóa giỏ") || lower.equals("bỏ giỏ") || lower.equals("hủy giỏ")
                || lower.equals("xóa hết") || lower.equals("bỏ hết") || lower.equals("clear cart")
                || lower.equals("reset cart") || lower.equals("xóa đơn") || lower.equals("hủy đơn")) {
            return DetectedIntent.clearCart();
        }

        // BEST SELLER
        if (isBestSellerRequest(lower, msg)) {
            String category = extractCategoryAfterKeyword(lower, msg, "bán chạy", "best seller", "hot");
            return DetectedIntent.bestSellers(category);
        }

        // CHECKOUT / PAYMENT
        if (lower.equals("thanh toán") || lower.equals("thanh toan") || lower.equals("checkout")
                || lower.equals("pay") || lower.equals("payment") || lower.equals("qr")
                || lower.equals("chốt đơn") || lower.equals("đặt đơn")
                || lower.equals("thanh toán đi") || lower.equals("thanh toán nha")
                || lower.equals("thanh toán đi mẹ") || lower.equals("thanh toán luôn")
                || lower.startsWith("thanh toán")) {
            return DetectedIntent.checkout();
        }

        // SIZE-ONLY responses
        if (isSizeOnly(lower)) {
            return DetectedIntent.sizeOnly(msg);
        }

        // YES / NO confirmations
        if (isAffirm(lower)) {
            return DetectedIntent.affirm();
        }
        if (isNegate(lower)) {
            return DetectedIntent.negate();
        }

        // No match — let Gemini handle it (find_item_by_name, etc.)
        return null;
    }

    // ── Simple predicate methods (no regex) ───────────────────────

    private boolean isGreeting(String lower) {
        return lower.equals("xin chào") || lower.equals("chào") || lower.equals("chào bạn")
                || lower.equals("chào mẹ") || lower.equals("hello") || lower.equals("hi")
                || lower.equals("hey") || lower.equals("helo")
                || lower.equals("good morning") || lower.equals("good afternoon")
                || lower.equals("good evening")
                || lower.equals("buổi sáng") || lower.equals("buổi trưa") || lower.equals("buổi chiều")
                || lower.matches("^hi\\b.*") || lower.matches("^hey.*")
                || lower.matches(".*\\bhi\\b.*") || lower.matches(".*\\bhey\\b.*");
    }

    private boolean isMenuRequest(String lower, String original) {
        // Exact matches
        if (lower.equals("menu") || lower.equals("thực đơn") || lower.equals("thuc don")
                || lower.equals("danh sach mon") || lower.equals("danh sách món")
                || lower.equals("list món") || lower.equals("list mon")) {
            return true;
        }
        // Starts with keywords
        if (lower.startsWith("xem menu") || lower.startsWith("xem thực đơn")
                || lower.startsWith("xem thuc don") || lower.startsWith("xem danh sách")
                || lower.startsWith("xem danh sach") || lower.startsWith("menu ")) {
            return true;
        }
        // Contains menu keyword (standalone word)
        String[] words = lower.split("\\s+");
        for (String w : words) {
            if (w.equals("menu") || w.equals("thực") || w.equals("thuc")
                    || w.equals("đơn") || w.equals("don")) {
                return true;
            }
        }
        return false;
    }

    private boolean isCartViewRequest(String lower, String original) {
        if (lower.equals("giỏ hàng") || lower.equals("gio hang") || lower.equals("cart")
                || lower.equals("bill") || lower.equals("hóa đơn") || lower.equals("hoa don")
                || lower.equals("xem giỏ") || lower.equals("xem gio") || lower.equals("xem cart")
                || lower.equals("xem đơn") || lower.equals("xem don") || lower.equals("xem bill")
                || lower.equals("view cart") || lower.equals("check cart")
                || lower.equals("giỏ") || lower.equals("gio") || lower.equals("đơn") || lower.equals("don")) {
            return true;
        }
        // "giỏ hàng của tôi", "xem đơn của mình", etc.
        if ((lower.contains("giỏ") || lower.contains("gio") || lower.contains("đơn") || lower.contains("don")
                || lower.contains("bill") || lower.contains("cart"))
                && (lower.contains("tôi") || lower.contains("mình") || lower.contains("của")
                    || lower.startsWith("giỏ") || lower.startsWith("gio")
                    || lower.startsWith("xem"))) {
            return true;
        }
        return false;
    }

    private boolean isBestSellerRequest(String lower, String original) {
        if (lower.equals("bán chạy") || lower.equals("ban chay")
                || lower.equals("best seller") || lower.equals("hot")
                || lower.equals("nổi bật") || lower.equals("noi bat")
                || lower.equals("top món") || lower.equals("top mon")
                || lower.equals("menu hot") || lower.equals("món hot")) {
            return true;
        }
        if (lower.startsWith("bán chạy") || lower.startsWith("best seller")
                || lower.startsWith("bán chạy")) {
            return true;
        }
        return false;
    }

    private boolean isSizeOnly(String lower) {
        // Single word size responses
        if (lower.equals("m") || lower.equals("l") || lower.equals("size m") || lower.equals("size l")
                || lower.equals("size m)")) {  // handles "size M)"
            return true;
        }
        // "lớn" = L, "nhỏ" = M
        if (lower.equals("lớn") || lower.equals("lon") || lower.equals("lả")) {
            return true;
        }
        if (lower.equals("nhỏ") || lower.equals("nho") || lower.equals("vừa") || lower.equals("vua")) {
            return true;
        }
        return false;
    }

    private boolean isAffirm(String lower) {
        return lower.equals("đúng") || lower.equals("dung") || lower.equals("rồi") || lower.equals("roi")
                || lower.equals("ok") || lower.equals("okay") || lower.equals("oke") || lower.equals("ỏk")
                || lower.equals("yes") || lower.equals("yep") || lower.equals("yeah") || lower.equals("ya")
                || lower.equals("đồng ý") || lower.equals("dong y") || lower.equals("có đúng")
                || lower.equals("có") || lower.equals("co")
                || lower.equals("vâng") || lower.equals("vang") || lower.equals("dạ") || lower.equals("da")
                || lower.equals("được") || lower.equals("duoc") || lower.equals("ừ") || lower.equals("u")
                || lower.equals("ừm") || lower.equals("um") || lower.equals("uhu") || lower.equals("u hu")
                || lower.matches("^đúng rồi$") || lower.matches(".*ok.*")
                || lower.matches(".*được$") || lower.matches(".*vâng$");
    }

    private boolean isNegate(String lower) {
        return lower.equals("không") || lower.equals("khong") || lower.equals("sai")
                || lower.equals("sai rồi") || lower.equals("no") || lower.equals("nope")
                || lower.equals("không đúng") || lower.equals("thôi") || lower.equals("bỏ")
                || lower.equals("không cần") || lower.equals("khong can")
                || lower.matches(".*không.*");
    }

    /**
     * Extract category after a keyword like "menu", "thực đơn", etc.
     * e.g. "menu tra sua" → "Tra Sua"
     */
    private String extractCategoryAfterKeyword(String lower, String original,
            String... keywords) {
        for (String kw : keywords) {
            if (lower.startsWith(kw + " ") || lower.contains(kw + " ")) {
                int idx = lower.indexOf(kw + " ");
                String after = original.substring(idx + kw.length()).trim();
                if (!after.isEmpty()) {
                    // Capitalize properly
                    return capitalizeCategory(after);
                }
            }
        }
        return null;
    }

    private String capitalizeCategory(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] validCategories = {
                "Trà Sữa", "Trà Sữa", "Trà Trái Cây", "Trà Trái Cây",
                "Cà Phê", "Cà Phê", "Cà Phê", "Cà Phê",
                "Đá Xay", "Đá Xay",
                "Topping", "Topping"
        };
        String lower = s.toLowerCase();
        if (lower.equals("trà sữa") || lower.equals("tra sua") || lower.equals("trasua")) return "Trà Sữa";
        if (lower.equals("trà trái cây") || lower.equals("tra trai cay") || lower.equals("tratraicay")) return "Trà Trái Cây";
        if (lower.equals("cà phê") || lower.equals("ca phe") || lower.equals("caphe") || lower.equals("càfe")) return "Cà Phê";
        if (lower.equals("đá xay") || lower.equals("da xay") || lower.equals("daxay")) return "Đá Xay";
        if (lower.equals("topping") || lower.equals("toppings")) return "Topping";
        return s;
    }

    // ══════════════════════════════════════════════════════════════
    // DetectedIntent — result type
    // ══════════════════════════════════════════════════════════════

    public static class DetectedIntent {
        public enum Type {
            // Tool calls — handled without Gemini
            MENU, VIEW_CART, CLEAR_CART, BEST_SELLERS, CHECKOUT,
            // Non-tool — respond directly
            GREETING, HELP, AFFIRM, NEGATE, SIZE_ONLY,
            // Needs Gemini reasoning
            COMPLEX
        }

        public final Type type;
        public final String category;   // for MENU / BEST_SELLERS
        public final String rawMessage; // original message

        private DetectedIntent(Type type, String category, String rawMessage) {
            this.type = type;
            this.category = category;
            this.rawMessage = rawMessage;
        }

        public static DetectedIntent menu(String category) {
            return new DetectedIntent(Type.MENU, category, null);
        }
        public static DetectedIntent viewCart() {
            return new DetectedIntent(Type.VIEW_CART, null, null);
        }
        public static DetectedIntent clearCart() {
            return new DetectedIntent(Type.CLEAR_CART, null, null);
        }
        public static DetectedIntent bestSellers(String category) {
            return new DetectedIntent(Type.BEST_SELLERS, category, null);
        }
        public static DetectedIntent checkout() {
            return new DetectedIntent(Type.CHECKOUT, null, null);
        }
        public static DetectedIntent greeting() {
            return new DetectedIntent(Type.GREETING, null, null);
        }
        public static DetectedIntent help() {
            return new DetectedIntent(Type.HELP, null, null);
        }
        public static DetectedIntent affirm() {
            return new DetectedIntent(Type.AFFIRM, null, null);
        }
        public static DetectedIntent negate() {
            return new DetectedIntent(Type.NEGATE, null, null);
        }
        public static DetectedIntent sizeOnly(String raw) {
            return new DetectedIntent(Type.SIZE_ONLY, null, raw);
        }

        public boolean isToolCall() {
            return type == Type.MENU || type == Type.VIEW_CART
                    || type == Type.CLEAR_CART || type == Type.BEST_SELLERS
                    || type == Type.CHECKOUT;
        }
    }
}
