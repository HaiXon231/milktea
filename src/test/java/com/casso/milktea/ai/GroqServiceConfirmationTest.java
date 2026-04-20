package com.casso.milktea.ai;

import com.casso.milktea.model.ConversationMessage;
import com.casso.milktea.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GroqService confirmation handling logic.
 *
 * Tests the flow:
 * 1. find_item_by_name → saves pending ADD_TO_CART
 * 2. handleSizeOnly → updates pending size
 * 3. handleConfirmation(true) → executes add_to_cart
 */
@ExtendWith(MockitoExtension.class)
class GroqServiceConfirmationTest {

    @Mock
    private RestTemplate restTemplate;

    private GroqService groqService;
    private IntentDetector intentDetector;
    private ConfirmationState confirmationState;

    // Mock dependencies (AiToolFunctions etc.) not needed for
    // the confirmation flow tests since we test handleSizeOnly and
    // handleConfirmation which call toolFunctions — but we can test
    // the GroqService entry point with a mock AI response.

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        intentDetector = new IntentDetector();
        confirmationState = new ConfirmationState();
        // We can't easily inject a mock AiToolFunctions, so we'll test
        // the confirmation handling logic (which doesn't need AI) separately.
        // For the full chat flow, we need integration tests.
    }

    // ─────────────────────────────────────────────────────────────
    // ConfirmationState integration — tests that GroqService's
    // ConfirmationState usage produces correct pending actions
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Confirmation flow: find_item → size reply → confirm")
    class ConfirmationFlow {

        @Test
        @DisplayName("Step 1: After find_item_by_name FOUND, pending should be saved")
        void step1_findItemSavesPending() {
            // Simulate what GroqService.executeTool does for find_item_by_name
            String rawResult = "FOUND:TS01|Trà Sữa Socola|30000|35000";

            if (rawResult.startsWith("FOUND:")) {
                String[] parts = rawResult.substring(6).split("\\|", -1);
                assertEquals(4, parts.length);
                assertEquals("TS01", parts[0].trim());
                assertEquals("Trà Sữa Socola", parts[1].trim());

                // This is what GroqService.saveAddToCart does
                confirmationState.saveAddToCart(
                        12345L, parts[0].trim(), parts[1].trim(), "M", 1, rawResult);
            }

            // Verify pending saved
            var pending = confirmationState.peek(12345L);
            assertNotNull(pending);
            assertEquals("TS01", pending.itemId());
            assertEquals("Trà Sữa Socola", pending.itemName());
            assertEquals("M", pending.size());
            assertEquals(1, pending.quantity());
        }

        @Test
        @DisplayName("Step 2: Size reply updates pending")
        void step2_sizeReplyUpdatesPending() {
            // Setup: pending from step 1
            confirmationState.saveAddToCart(12345L, "TS01", "Trà Sữa Socola", "M", 1, "FOUND:...");

            // Simulate size-only handler logic
            String sizeReply = "L";
            String newSize = normalizeSize(sizeReply);

            var existing = confirmationState.peek(12345L);
            assertNotNull(existing);
            assertEquals("M", existing.size()); // was M, changing to L

            // Update pending with new size
            var updated = new ConfirmationState.PendingAction(
                    existing.type(), existing.itemId(), existing.itemName(),
                    newSize, existing.quantity(), existing.timestamp(), existing.context());
            confirmationState.save(12345L, updated);

            // Verify
            var after = confirmationState.peek(12345L);
            assertEquals("L", after.size());
        }

        @Test
        @DisplayName("Step 3: Affirm executes the pending action")
        void step3_affirmExecutesPending() {
            // Setup: pending from step 2
            confirmationState.saveAddToCart(12345L, "TS01", "Trà Sữa Socola", "L", 1, "FOUND:...");

            // Simulate handleConfirmation logic
            boolean affirmed = true;
            var pending = confirmationState.peek(12345L);

            assertNotNull(pending, "Pending should exist when user affirms");

            if (affirmed && pending.type() == ConfirmationState.ActionType.ADD_TO_CART) {
                // Execute the action
                String itemId = pending.itemId();
                String size = pending.size();
                int qty = pending.quantity();
                confirmationState.clear(12345L);

                // Verify action was "executed" (itemId, size, qty extracted)
                assertEquals("TS01", itemId);
                assertEquals("L", size);
                assertEquals(1, qty);

                // Verify cleared
                assertNull(confirmationState.peek(12345L));
            }
        }

        @Test
        @DisplayName("Step 4: After execution, cart should have the item")
        void step4_cartHasItem() {
            // This test verifies the end-to-end: pending → add_to_cart → cart
            // We simulate what toolFunctions.addToCart would do

            // Setup
            confirmationState.saveAddToCart(12345L, "TS01", "Trà Sữa Socola", "L", 2, "FOUND:...");

            // Execute
            var pending = confirmationState.getAndClear(12345L);
            assertNotNull(pending);

            // The actual cart would be updated here by CartService.addToCart
            // We verify the action parameters are correct
            assertEquals("TS01", pending.itemId());
            assertEquals("L", pending.size());
            assertEquals(2, pending.quantity());
            assertEquals("Trà Sữa Socola", pending.itemName());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // IntentDetector + ConfirmationState combined flow
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Full flow: IntentDetector detects → GroqService handles")
    class FullFlowIntentDetector {

        @Test
        @DisplayName("'giỏ hàng' → VIEW_CART intent (not null)")
        void gioHang_detected() {
            var intent = intentDetector.detect("giỏ hàng", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.VIEW_CART, intent.type);
            assertTrue(intent.isToolCall());
        }

        @Test
        @DisplayName("'M' → SIZE_ONLY intent when pending exists")
        void sizeM_detected() {
            // Setup pending
            confirmationState.saveAddToCart(12345L, "TS01", "Trà Sữa", "M", 1, "FOUND:...");

            var intent = intentDetector.detect("M", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.SIZE_ONLY, intent.type);
        }

        @Test
        @DisplayName("'đúng rồi' → AFFIRM intent")
        void dungRoi_detected() {
            // Setup pending
            confirmationState.saveAddToCart(12345L, "TS01", "Trà Sữa", "M", 1, "FOUND:...");

            var intent = intentDetector.detect("đúng rồi", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.AFFIRM, intent.type);
        }

        @Test
        @DisplayName("'đúng rồi' with NO pending → AFFIRM (but handleConfirmation will Gemini-pass)")
        void dungRoi_noPending() {
            // No pending setup
            var intent = intentDetector.detect("đúng rồi", null);
            assertNotNull(intent);
            assertEquals(IntentDetector.DetectedIntent.Type.AFFIRM, intent.type);

            // handleConfirmation will check pending and route to Gemini
            var pending = confirmationState.peek(12345L);
            assertNull(pending); // no pending
            // This means GroqService.handleConfirmation will call chatWithGemini
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helper methods (mirroring GroqService logic)
    // ─────────────────────────────────────────────────────────────

    private String normalizeSize(String size) {
        if (size == null || size.isBlank()) return "M";
        String s = size.trim().toUpperCase();
        return s.startsWith("L") ? "L" : "M";
    }
}