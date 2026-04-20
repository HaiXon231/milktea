package com.casso.milktea.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfirmationState.
 * Verifies pending action save/retrieve/clear behavior.
 */
class ConfirmationStateTest {

    private ConfirmationState state;

    @BeforeEach
    void setUp() {
        state = new ConfirmationState();
    }

    // ── Save & Retrieve ───────────────────────────────────────────

    @Test
    @DisplayName("saveAddToCart should be retrievable by peek")
    void saveAddToCart_peekable() {
        Long customerId = 12345L;
        state.saveAddToCart(customerId, "TS01", "Trà Sữa Socola", "M", 2, "FOUND:TS01|...");

        var pending = state.peek(customerId);
        assertNotNull(pending);
        assertEquals(ConfirmationState.ActionType.ADD_TO_CART, pending.type());
        assertEquals("TS01", pending.itemId());
        assertEquals("Trà Sữa Socola", pending.itemName());
        assertEquals("M", pending.size());
        assertEquals(2, pending.quantity());
    }

    @Test
    @DisplayName("getAndClear should remove the pending action")
    void getAndClear_removes() {
        Long customerId = 12345L;
        state.saveAddToCart(customerId, "TS01", "Trà Sữa", "M", 1, "FOUND:...");

        var pending = state.getAndClear(customerId);
        assertNotNull(pending);

        // Second call should return null
        assertNull(state.peek(customerId));
    }

    @Test
    @DisplayName("clear should remove pending action")
    void clear_removes() {
        Long customerId = 12345L;
        state.saveAddToCart(customerId, "TS01", "Trà Sữa", "M", 1, "FOUND:...");
        assertTrue(state.hasPending(customerId));

        state.clear(customerId);
        assertFalse(state.hasPending(customerId));
        assertNull(state.peek(customerId));
    }

    @Test
    @DisplayName("hasPending returns true only when pending exists")
    void hasPending() {
        Long customerId = 12345L;
        assertFalse(state.hasPending(customerId));

        state.saveAddToCart(customerId, "TS01", "Trà Sữa", "M", 1, "FOUND:...");
        assertTrue(state.hasPending(customerId));

        state.clear(customerId);
        assertFalse(state.hasPending(customerId));
    }

    @Test
    @DisplayName("peek on unknown customer returns null")
    void peek_unknown() {
        assertNull(state.peek(99999L));
        assertFalse(state.hasPending(99999L));
    }

    @Test
    @DisplayName("save() with PendingAction should store it directly")
    void save_direct() {
        Long customerId = 12345L;
        var action = new ConfirmationState.PendingAction(
                ConfirmationState.ActionType.ADD_TO_CART,
                "MATCHA01", "Matcha", "L", 3,
                System.currentTimeMillis(),
                "FOUND:MATCHA01|Matcha|35000|40000");
        state.save(customerId, action);

        var result = state.peek(customerId);
        assertNotNull(result);
        assertEquals("MATCHA01", result.itemId());
        assertEquals("L", result.size());
        assertEquals(3, result.quantity());
    }
}
