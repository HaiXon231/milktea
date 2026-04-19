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
     * Create an order from the customer's cart and generate a payOS payment link.
     */
    @Transactional
    public OrderResult checkout(Customer customer, String note) {
        List<CartItem> cartItems = cartService.getCartItems(customer);

        if (cartItems.isEmpty()) {
            return new OrderResult(false, "Giỏ hàng trống! Hãy thêm món trước khi thanh toán.", null, null);
        }

        int totalAmount = cartService.calculateTotal(cartItems);

        // Generate unique order code (timestamp-based)
        long orderCode = System.currentTimeMillis() / 1000;

        // Create order
        Order order = Order.builder()
                .orderCode(orderCode)
                .customer(customer)
                .totalAmount(totalAmount)
                .status(OrderStatus.AWAITING_PAYMENT)
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
                    .description("Don hang #" + orderCode)
                    .returnUrl(appBaseUrl + "/payment/success")
                    .cancelUrl(appBaseUrl + "/payment/cancel")
                    .items(payosItems)
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);

            order.setPaymentUrl(response.getCheckoutUrl());
            orderRepository.save(order);

            // Clear the cart
            cartService.clearCart(customer);

            // Build response
            StringBuilder sb = new StringBuilder();
            sb.append("📋 ĐƠN HÀNG #").append(orderCode).append("\n\n");
            for (OrderItem item : order.getItems()) {
                sb.append(String.format("• %dx %s (size %s) - %,dđ\n",
                        item.getQuantity(), item.getItemName(),
                        item.getSize(), item.getSubtotal()));
            }
            sb.append(String.format("\n💰 Tổng cộng: %,dđ\n", totalAmount));
            sb.append("\n💳 Thanh toán tại link bên dưới:");

            return new OrderResult(true, sb.toString(), response.getCheckoutUrl(), orderCode);

        } catch (Exception e) {
            log.error("Failed to create payOS payment link for order {}", orderCode, e);
            order.setStatus(OrderStatus.PENDING);
            orderRepository.save(order);
            cartService.clearCart(customer);
            return new OrderResult(false,
                    "Đã tạo đơn hàng #" + orderCode + " nhưng lỗi tạo link thanh toán. Vui lòng liên hệ quán.",
                    null, orderCode);
        }
    }

    /**
     * Confirm payment via webhook.
     */
    @Transactional
    public Order confirmPayment(long orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
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
    public record OrderResult(boolean success, String message, String paymentUrl, Long orderCode) {}
}
