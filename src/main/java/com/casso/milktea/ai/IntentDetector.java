package com.casso.milktea.ai;

import com.casso.milktea.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Fast-path intent detector — runs BEFORE Gemini for high-confidence intents.
 *
 * Solves the problem where Gemini misinterprets simple, unambiguous requests
 * like "menu", "gio hang", "thanh toan" as find_item_by_name queries.
 *
 * IntentDetector returns a ToolRequest for simple intents, null for everything else
 * (which then goes to Gemini for complex reasoning).
 */
@Component
@RequiredArgsConstructor
public class IntentDetector {

    // ── Intent patterns (order matters — more specific first) ───

    // MENU intents
    private static final Pattern PAT_MENU = Pattern.compile(
            "(?i)^\\s*(menu|thuc\\s*don|danh\\s*sach\\s*mon|xem\\s*menu|xem\\s*thuc\\s*don|xem\\s*danh\\s*sach|lIST\\s*mon)\\s*$"
    );
    private static final Pattern PAT_MENU_CAT = Pattern.compile(
            "(?i)^\\s*(menu|thuc\\s*don)\\s+(.+)$"
    );

    // CART intents
    private static final Pattern PAT_VIEW_CART = Pattern.compile(
            "(?i)^\\s*(gio\\s*hang|gio\\s*hàng|cart|bill|hoa\\s*don|xem\\s*gio|xem\\s*cart|xem\\s*đơn|xem\\s*don|kiểm\\s*tra\\s*gio|check\\s*cart|view\\s*cart)\\s*$"
    );
    private static final Pattern PAT_CLEAR_CART = Pattern.compile(
            "(?i)^\\s*(xóa\\s*gio|xóa\\s*hết|bỏ\\s*gio|hủy\\s*gio|clear\\s*cart|reset\\s*cart)\\s*$"
    );

    // BEST SELLER
    private static final Pattern PAT_BEST_SELLER = Pattern.compile(
            "(?i)^\\s*(bán\\s*chạy|best\\s*seller|hot|nổi\\s*bật|top\\s*mon|menu\\s*hot)\\s*$"
    );
    private static final Pattern PAT_BEST_SELLER_CAT = Pattern.compile(
            "(?i)^\\s*(bán\\s*chạy|best\\s*seller)\\s+(.+)$"
    );

    // CHECKOUT / PAYMENT
    private static final Pattern PAT_CHECKOUT = Pattern.compile(
            "(?i)^\\s*(thanh\\s*toán|thanh\\s*toan|checkout|pay|qr|chốt\\s*đơn|đặt\\s*đơn|payment)\\s*$"
    );

    // GREETING (also handled in GroqService but IntentDetector catches it first)
    private static final Pattern PAT_GREETING = Pattern.compile(
            "(?i)^\\s*(xin\\s*chao|chào|hello|hi\\b|hey|good\\s*morning|good\\s*afternoon|buổi\\s*sáng|buổi\\s*trưa|buổi\\s*chiều)\\s*$"
    );

    // HELP / QUESTION about ordering
    private static final Pattern PAT_HELP = Pattern.compile(
            "(?i)^\\s*(mẹ\\s*ơi|mame|help|giúp\\s*mẹ|bạn\\s*là\\s*ai|bạn\\s*là\\s*gì)\\s*$"
    );

    // YES/NO confirmation patterns
    private static final Pattern PAT_YES = Pattern.compile(
            "(?i)^\\s*(đúng|rồi|ok|okay|yes|yep|yeah|đồng\\s*ý|có\\s*đúng|vâng|dạ|được|ừ|ừm|uhu|u\\b)\\s*$"
    );
    private static final Pattern PAT_NO = Pattern.compile(
            "(?i)^\\s*(không|sai|sai\\s*rồi|no|nope|nope\\b|không\\s*đúng|thôi|bỏ|k\\s*bỏ)\\s*$"
    );

    // Size changes
    private static final Pattern PAT_CHANGE_SIZE = Pattern.compile(
            "(?i)^\\s*(size\\s*[MLml]|lớn|nhỏ|đổi\\s*size|đổi\\s*sang)\\s*$"
    );

    /**
     * Detect intent from user message.
     * Returns a detected intent, or null if needs Gemini reasoning.
     */
    public DetectedIntent detect(String userMessage, Customer customer) {
        if (userMessage == null || userMessage.isBlank()) {
            return null;
        }
        String msg = userMessage.trim();

        // GREETING
        if (PAT_GREETING.matcher(msg).matches()) {
            return DetectedIntent.greeting();
        }

        // HELP
        if (PAT_HELP.matcher(msg).matches()) {
            return DetectedIntent.help();
        }

        // MENU — full menu
        if (PAT_MENU.matcher(msg).matches()) {
            return DetectedIntent.menu(null);
        }

        // MENU — with category
        var menuCatMatcher = PAT_MENU_CAT.matcher(msg);
        if (menuCatMatcher.matches()) {
            String category = menuCatMatcher.group(2).trim();
            return DetectedIntent.menu(category);
        }

        // BEST SELLER — no category
        if (PAT_BEST_SELLER.matcher(msg).matches()) {
            return DetectedIntent.bestSellers(null);
        }

        // BEST SELLER — with category
        var bsMatcher = PAT_BEST_SELLER_CAT.matcher(msg);
        if (bsMatcher.matches()) {
            String category = bsMatcher.group(2).trim();
            return DetectedIntent.bestSellers(category);
        }

        // VIEW CART
        if (PAT_VIEW_CART.matcher(msg).matches()) {
            return DetectedIntent.viewCart();
        }

        // CLEAR CART
        if (PAT_CLEAR_CART.matcher(msg).matches()) {
            return DetectedIntent.clearCart();
        }

        // CHECKOUT
        if (PAT_CHECKOUT.matcher(msg).matches()) {
            return DetectedIntent.checkout();
        }

        // Size-only response (e.g., "L", "lớn", "size M")
        if (PAT_CHANGE_SIZE.matcher(msg).matches()) {
            return DetectedIntent.sizeOnly(msg);
        }

        // YES — user confirms something
        if (PAT_YES.matcher(msg).matches()) {
            return DetectedIntent.affirm();
        }

        // NO — user rejects or changes mind
        if (PAT_NO.matcher(msg).matches()) {
            return DetectedIntent.negate();
        }

        // Unknown — let Gemini handle it
        return null;
    }

    /**
     * Detected intent result.
     * Can represent:
     * - A tool to call directly (FAST PATH)
     * - A non-tool intent (greeting, help, yes/no)
     * - A request to use Gemini (type = COMPLEX)
     */
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

        public String toolName() {
            return switch (type) {
                case MENU -> "get_menu";
                case VIEW_CART -> "view_cart";
                case CLEAR_CART -> "clear_cart";
                case BEST_SELLERS -> "get_best_sellers";
                case CHECKOUT -> "checkout";
                default -> null;
            };
        }
    }
}
