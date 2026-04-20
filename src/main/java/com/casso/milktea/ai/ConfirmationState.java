package com.casso.milktea.ai;

import com.casso.milktea.model.Customer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks pending confirmations per customer.
 *
 * When the AI asks a confirmation question (e.g., "Thêm trà sữa socola size M như vậy nhe con?"),
 * we save the pending action so that if the user says "đúng rồi" or "ok",
 * we know exactly what to do next.
 *
 * Each entry expires after 5 minutes to prevent stale state.
 */
@Component
public class ConfirmationState {

    private final ConcurrentHashMap<Long, PendingAction> pending = new ConcurrentHashMap<>();
    private static final long EXPIRE_MS = 5 * 60 * 1000L; // 5 minutes

    // ── Pending action record ─────────────────────────────────────

    /**
     * Represents a pending action waiting for user confirmation.
     */
    public record PendingAction(
            ActionType type,
            String itemId,
            String itemName,
            String size,
            int quantity,
            long timestamp,
            String context // raw context string
    ) {
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXPIRE_MS;
        }
    }

    public enum ActionType {
        ADD_TO_CART,     // User confirmed add-to-cart
        UPDATE_CART,     // User confirmed update quantity
        CHECKOUT,        // User confirmed checkout
        REMOVE_FROM_CART // User confirmed removal
    }

    // ── Save pending actions ─────────────────────────────────────

    /** Save a pending add-to-cart confirmation. */
    public void saveAddToCart(Long customerId, String itemId, String itemName,
            String size, int quantity, String context) {
        pending.put(customerId,
                new PendingAction(ActionType.ADD_TO_CART, itemId, itemName,
                        size, quantity, System.currentTimeMillis(), context));
    }

    /** Save a pending checkout confirmation (collect delivery info). */
    public void saveCheckoutInfo(Long customerId,
            String name, String phone, String address, String note) {
        pending.put(customerId,
                new PendingAction(ActionType.CHECKOUT, name, phone,
                        address, 0, System.currentTimeMillis(), note));
    }

    // ── Retrieve & clear ─────────────────────────────────────────

    public PendingAction getAndClear(Long customerId) {
        return pending.remove(customerId);
    }

    public PendingAction peek(Long customerId) {
        PendingAction action = pending.get(customerId);
        if (action != null && action.isExpired()) {
            pending.remove(customerId);
            return null;
        }
        return action;
    }

    public void clear(Long customerId) {
        pending.remove(customerId);
    }

    public boolean hasPending(Long customerId) {
        return peek(customerId) != null;
    }

    public void save(Long customerId, PendingAction action) {
        pending.put(customerId, action);
    }
}
