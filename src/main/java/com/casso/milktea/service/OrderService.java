package com.casso.milktea.service;

import com.casso.milktea.model.*;
import com.casso.milktea.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final PayOS payOS;

    @Value("${app.base-url}")
    private String appBaseUrl;

    /**
     * Chốt đơn hàng + tạo link thanh toán payOS QR.
     *
     * @param name    Tên người nhận
     * @param phone    SĐT người nhận
     * @param address  Địa chỉ giao hàng
     * @param note     Ghi chú (tuỳ chọn)
     */
    @Transactional
    public OrderResult checkout(Customer customer, String name, String phone,
            String address, String note) {
        List<CartItem> cartItems = cartService.getCartItems(customer);

        if (cartItems.isEmpty()) {
            return new OrderResult(false,
                    "Giỏ hàng trống rồi con ơi! Thêm món vào giỏ trước nha 😊",
                    null, null);
        }

        int totalAmount = cartService.calculateTotal(cartItems);

        // Generate unique order code
        long orderCode = System.currentTimeMillis() / 1000;

        // Create order
        Order order = Order.builder()
                .orderCode(orderCode)
                .customer(customer)
                .totalAmount(totalAmount)
                .status(OrderStatus.AWAITING_PAYMENT)
                .deliveryName(name)
                .deliveryPhone(phone)
                .deliveryAddress(address)
                .note(note)
                .build();

        // Snapshot cart items into order items
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .itemId(cartItem.getMenuItem().getItemId())
                    .itemName(cartItem.getMenuItem().getName())
                    .size(cartItem.getSize())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .build();
            order.getItems().add(orderItem);
        }

        // Build description for payOS
        int itemCount = cartItems.stream()
                .mapToInt(com.casso.milktea.model.CartItem::getQuantity)
                .sum();

        // Create payOS payment link
        try {
            List<PaymentLinkItem> payosItems = cartItems.stream()
                    .map(ci -> PaymentLinkItem.builder()
                            .name(ci.getMenuItem().getName() + " (" + ci.getSize() + ")")
                            .quantity(ci.getQuantity())
                            .price((long) ci.getUnitPrice())
                            .build())
                    .toList();

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount((long) totalAmount)
                    .description("Casso #" + orderCode + " (" + itemCount + " món)")
                    .returnUrl(appBaseUrl + "/payment/success")
                    .cancelUrl(appBaseUrl + "/payment/cancel")
                    .items(payosItems)
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);

            order.setPaymentUrl(response.getCheckoutUrl());
            orderRepository.save(order);

            // Clear the cart ONLY after successfully creating payment link
            cartService.clearCart(customer);

            // Build order summary
            StringBuilder sb = new StringBuilder();
            sb.append("📋 ĐƠN HÀNG #").append(orderCode).append("\n");
            sb.append("👤 Người nhận: ").append(name).append("\n");
            sb.append("📞 SĐT: ").append(phone).append("\n");
            sb.append("📍 Địa chỉ: ").append(address).append("\n\n");
            for (OrderItem item : order.getItems()) {
                sb.append(String.format("• %dx %s (size %s) - %,dđ\n",
                        item.getQuantity(), item.getItemName(),
                        item.getSize(), item.getSubtotal()));
            }
            sb.append(String.format("\n💰 Tổng cộng: %,dđ\n", totalAmount));
            sb.append("\n💳 LINK THANH TOÁN QR:\n").append(response.getCheckoutUrl());
            sb.append("\n\nCon nhấn vào link trên để quét QR thanh toán nha!");

            return new OrderResult(true, sb.toString(), response.getCheckoutUrl(), orderCode);

        } catch (Exception e) {
            log.error("Failed to create payOS payment link for order {}", orderCode, e);
            // DO NOT clear cart on failure — user can retry with the same cart
            order.setStatus(OrderStatus.PENDING);
            orderRepository.save(order);
            return new OrderResult(false,
                    "Tạo link thanh toán thất bại: " + e.getMessage() + ". "
                            + "Con nhắn lại 'thanh toán' để thử lại nha! Đơn hàng vẫn còn trong giỏ 😊",
                    null, orderCode);
        }
    }

    /**
     * Confirm payment via payOS webhook.
     */
    @Transactional
    public Order confirmPayment(long orderCode) {
        Order order = orderRepository.findByOrderCodeWithCustomer(orderCode)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderCode));

        if (order.getStatus() == OrderStatus.PAID) {
            log.warn("Order {} already paid, ignoring duplicate webhook", orderCode);
            return order;
        }

        order.setStatus(OrderStatus.PAID);
        return orderRepository.save(order);
    }

    /**
     * Result record for checkout operation.
     */
    public record OrderResult(boolean success, String message, String paymentUrl, Long orderCode) {

        public String toToolResult() {
            return message;
        }
    }
}