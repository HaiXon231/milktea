package com.casso.milktea.service;

import com.casso.milktea.model.CartItem;
import com.casso.milktea.model.Customer;
import com.casso.milktea.model.MenuItem;
import com.casso.milktea.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final MenuService menuService;

    /**
     * Add an item to the cart. If the same item+size already exists, increment quantity.
     */
    @Transactional
    public String addToCart(Customer customer, String itemId, String size, int quantity) {
        // Validate item exists and is available
        Optional<MenuItem> menuItemOpt = menuService.findAvailableItem(itemId);
        if (menuItemOpt.isEmpty()) {
            return "Lỗi: Món " + itemId + " không tồn tại hoặc đã hết hàng.";
        }

        // Validate size
        size = size.toUpperCase();
        if (!size.equals("M") && !size.equals("L")) {
            return "Lỗi: Size chỉ có M hoặc L.";
        }

        // Validate quantity
        if (quantity <= 0) {
            return "Lỗi: Số lượng phải lớn hơn 0.";
        }

        MenuItem menuItem = menuItemOpt.get();
        int unitPrice = menuItem.getPriceBySize(size);

        // Check if item+size already in cart
        Optional<CartItem> existingOpt = cartItemRepository
                .findByCustomerIdAndMenuItemItemIdAndSize(customer.getId(), itemId, size);

        if (existingOpt.isPresent()) {
            CartItem existing = existingOpt.get();
            existing.setQuantity(existing.getQuantity() + quantity);
            cartItemRepository.save(existing);
        } else {
            CartItem cartItem = CartItem.builder()
                    .customer(customer)
                    .menuItem(menuItem)
                    .size(size)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .build();
            cartItemRepository.save(cartItem);
        }

        return String.format("Đã thêm %dx %s (size %s) - %,dđ/ly vào giỏ hàng.",
                quantity, menuItem.getName(), size, unitPrice);
    }

    /**
     * View the current cart contents.
     */
    public String viewCart(Customer customer) {
        List<CartItem> items = cartItemRepository.findByCustomerIdWithMenuItem(customer.getId());

        if (items.isEmpty()) {
            return "Giỏ hàng trống. Chưa có món nào.";
        }

        StringBuilder sb = new StringBuilder("🛒 GIỎ HÀNG:\n");
        int total = 0;
        for (CartItem item : items) {
            int subtotal = item.getSubtotal();
            total += subtotal;
            sb.append(String.format("• %dx %s (size %s) - %,dđ\n",
                    item.getQuantity(), item.getMenuItem().getName(),
                    item.getSize(), subtotal));
        }
        sb.append(String.format("\n💰 TỔNG CỘNG: %,dđ", total));
        return sb.toString();
    }

    /**
     * Update the quantity of an item in the cart.
     */
    @Transactional
    public String updateCartItem(Customer customer, String itemId, String size, int quantity) {
        size = size.toUpperCase();
        Optional<CartItem> existingOpt = cartItemRepository
                .findByCustomerIdAndMenuItemItemIdAndSize(customer.getId(), itemId, size);

        if (existingOpt.isEmpty()) {
            return "Lỗi: Món " + itemId + " (size " + size + ") không có trong giỏ hàng.";
        }

        if (quantity <= 0) {
            cartItemRepository.delete(existingOpt.get());
            return "Đã xóa món khỏi giỏ hàng.";
        }

        CartItem existing = existingOpt.get();
        existing.setQuantity(quantity);
        cartItemRepository.save(existing);
        return String.format("Đã cập nhật %s (size %s) thành %d ly.",
                existing.getMenuItem().getName(), size, quantity);
    }

    /**
     * Remove an item from the cart.
     */
    @Transactional
    public String removeFromCart(Customer customer, String itemId, String size) {
        size = size.toUpperCase();
        Optional<CartItem> existingOpt = cartItemRepository
                .findByCustomerIdAndMenuItemItemIdAndSize(customer.getId(), itemId, size);

        if (existingOpt.isEmpty()) {
            return "Lỗi: Món " + itemId + " (size " + size + ") không có trong giỏ hàng.";
        }

        cartItemRepository.delete(existingOpt.get());
        return "Đã xóa " + existingOpt.get().getMenuItem().getName() + " (size " + size + ") khỏi giỏ hàng.";
    }

    /**
     * Get cart items for checkout.
     */
    public List<CartItem> getCartItems(Customer customer) {
        return cartItemRepository.findByCustomerIdWithMenuItem(customer.getId());
    }

    /**
     * Clear the cart after checkout.
     */
    @Transactional
    public void clearCart(Customer customer) {
        cartItemRepository.deleteByCustomerId(customer.getId());
    }

    /**
     * Calculate total amount.
     */
    public int calculateTotal(List<CartItem> items) {
        return items.stream()
                .mapToInt(CartItem::getSubtotal)
                .sum();
    }
}
