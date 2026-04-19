package com.casso.milktea.ai;

import com.casso.milktea.model.Customer;
import com.casso.milktea.model.MenuItem;
import com.casso.milktea.service.CartService;
import com.casso.milktea.service.MenuService;
import com.casso.milktea.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * AI Tool functions that OpenAI can call via Function Calling.
 * Each method is registered as a tool for the ChatClient.
 *
 * These are NOT Spring AI @Bean functions — they are called explicitly
 * via a manual function-calling dispatch in AiChatService, giving us
 * full control over the customer context injection.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiToolFunctions {

    private final MenuService menuService;
    private final CartService cartService;
    private final OrderService orderService;

    /**
     * Get the menu, optionally filtered by category.
     */
    public String getMenu(String category) {
        log.info("AI Tool: get_menu(category={})", category);
        List<MenuItem> items = menuService.getMenu(category);
        if (items.isEmpty()) {
            return "Không tìm thấy món nào" +
                    (category != null ? " trong danh mục " + category : "") + ".";
        }
        return menuService.formatMenuForAI(items);
    }

    /**
     * Add item to customer's cart.
     */
    public String addToCart(Customer customer, String itemId, String size, int quantity) {
        log.info("AI Tool: add_to_cart(customer={}, item={}, size={}, qty={})",
                customer.getId(), itemId, size, quantity);
        return cartService.addToCart(customer, itemId, size, quantity);
    }

    /**
     * View the customer's cart.
     */
    public String viewCart(Customer customer) {
        log.info("AI Tool: view_cart(customer={})", customer.getId());
        return cartService.viewCart(customer);
    }

    /**
     * Update an item's quantity in the cart.
     */
    public String updateCartItem(Customer customer, String itemId, String size, int quantity) {
        log.info("AI Tool: update_cart_item(customer={}, item={}, size={}, qty={})",
                customer.getId(), itemId, size, quantity);
        return cartService.updateCartItem(customer, itemId, size, quantity);
    }

    /**
     * Remove an item from the cart.
     */
    public String removeFromCart(Customer customer, String itemId, String size) {
        log.info("AI Tool: remove_from_cart(customer={}, item={}, size={})",
                customer.getId(), itemId, size);
        return cartService.removeFromCart(customer, itemId, size);
    }

    /**
     * Checkout: create order and payment link.
     */
    public OrderService.OrderResult checkout(Customer customer, String note) {
        log.info("AI Tool: checkout(customer={}, note={})", customer.getId(), note);
        return orderService.checkout(customer, note);
    }
}
