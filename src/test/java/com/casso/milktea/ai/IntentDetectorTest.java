package com.casso.milktea.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IntentDetector.
 * Tests all fast-path intent detection cases.
 */
class IntentDetectorTest {

    private IntentDetector detector;

    @BeforeEach
    void setUp() {
        detector = new IntentDetector();
    }

    // ── MENU intents ───────────────────────────────────────────────

    @Nested
    class MenuIntents {
        @Test
        @DisplayName("'menu' should be detected as MENU intent")
        void menu_exact() {
            var intent = detector.detect("menu", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.MENU, intent.type);
        }

        @Test
        @DisplayName("'thực đơn' should be detected as MENU intent")
        void thucdon() {
            var intent = detector.detect("thực đơn", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.MENU, intent.type);
        }

        @Test
        @DisplayName("'xem menu' should be detected as MENU intent")
        void xem_menu() {
            var intent = detector.detect("xem menu", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.MENU, intent.type);
        }

        @Test
        @DisplayName("'danh sách món' should be detected as MENU intent")
        void danh_sach_mon() {
            var intent = detector.detect("danh sách món", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.MENU, intent.type);
        }

        @Test
        @DisplayName("'XEM MENU' (uppercase) should be detected as MENU intent")
        void menu_uppercase() {
            var intent = detector.detect("XEM MENU", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.MENU, intent.type);
        }
    }

    // ── CART intents ───────────────────────────────────────────────

    @Nested
    class CartIntents {
        @Test
        @DisplayName("'giỏ hàng' should be detected as VIEW_CART intent")
        void gio_hang() {
            var intent = detector.detect("giỏ hàng", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.VIEW_CART, intent.type);
        }

        @Test
        @DisplayName("'bill' should be detected as VIEW_CART intent")
        void bill() {
            var intent = detector.detect("bill", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.VIEW_CART, intent.type);
        }

        @Test
        @DisplayName("'xem giỏ hàng' should be detected as VIEW_CART intent")
        void xem_gio_hang() {
            var intent = detector.detect("xem giỏ hàng", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.VIEW_CART, intent.type);
        }

        @Test
        @DisplayName("'cho tôi xem giỏ hàng' should be detected as VIEW_CART intent")
        void cho_xem_gio_hang() {
            var intent = detector.detect("cho tôi xem giỏ hàng", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.VIEW_CART, intent.type);
        }

        @Test
        @DisplayName("'check cart' should be detected as VIEW_CART intent")
        void check_cart() {
            var intent = detector.detect("check cart", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.VIEW_CART, intent.type);
        }
    }

    // ── SIZE responses ──────────────────────────────────────────────

    @Nested
    class SizeResponses {
        @Test
        @DisplayName("'M' alone should be detected as SIZE_ONLY intent")
        void size_m() {
            var intent = detector.detect("M", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.SIZE_ONLY, intent.type);
        }

        @Test
        @DisplayName("'L' alone should be detected as SIZE_ONLY intent")
        void size_l() {
            var intent = detector.detect("L", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.SIZE_ONLY, intent.type);
        }

        @Test
        @DisplayName("'lớn' should be detected as SIZE_ONLY intent")
        void lon() {
            var intent = detector.detect("lớn", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.SIZE_ONLY, intent.type);
        }
    }

    // ── CONFIRMATION intents ────────────────────────────────────────

    @Nested
    class ConfirmationIntents {
        @Test
        @DisplayName("'đúng rồi' should be detected as AFFIRM intent")
        void dung_roi() {
            var intent = detector.detect("đúng rồi", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.AFFIRM, intent.type);
        }

        @Test
        @DisplayName("'ok' should be detected as AFFIRM intent")
        void ok() {
            var intent = detector.detect("ok", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.AFFIRM, intent.type);
        }

        @Test
        @DisplayName("'vâng' should be detected as AFFIRM intent")
        void vang() {
            var intent = detector.detect("vâng", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.AFFIRM, intent.type);
        }

        @Test
        @DisplayName("'không' should be detected as NEGATE intent")
        void khong() {
            var intent = detector.detect("không", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.NEGATE, intent.type);
        }
    }

    // ── CHECKOUT ───────────────────────────────────────────────────

    @Nested
    class CheckoutIntents {
        @Test
        @DisplayName("'thanh toán' should be detected as CHECKOUT intent")
        void thanh_toan() {
            var intent = detector.detect("thanh toán", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.CHECKOUT, intent.type);
        }

        @Test
        @DisplayName("'checkout' should be detected as CHECKOUT intent")
        void checkout() {
            var intent = detector.detect("checkout", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.CHECKOUT, intent.type);
        }
    }

    // ── GREETING ───────────────────────────────────────────────────

    @Nested
    class GreetingIntents {
        @Test
        @DisplayName("'xin chào' should be detected as GREETING intent")
        void xin_chao() {
            var intent = detector.detect("xin chào", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.GREETING, intent.type);
        }

        @Test
        @DisplayName("'chào bạn' should be detected as GREETING intent")
        void chao_ban() {
            var intent = detector.detect("chào bạn", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.GREETING, intent.type);
        }
    }

    // ── Unknown intents → null (Gemini path) ───────────────────────

    @Nested
    class UnknownIntents {
        @Test
        @DisplayName("'trà sữa socola' should be null (needs Gemini for find_item)")
        void item_name() {
            var intent = detector.detect("trà sữa socola", null);
            assertNull(intent, "Item names should pass to Gemini");
        }

        @Test
        @DisplayName("'tôi muốn matcha' should be null (needs Gemini)")
        void preference() {
            var intent = detector.detect("tôi muốn matcha", null);
            assertNull(intent, "Preferences should pass to Gemini");
        }
    }
}
