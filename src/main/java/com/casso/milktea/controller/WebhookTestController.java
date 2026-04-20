package com.casso.milktea.controller;

import com.casso.milktea.bot.TeaShopBot;
import com.casso.milktea.model.Order;
import com.casso.milktea.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin/test endpoints for debugging payment and notification flows.
 *
 * IMPORTANT: Remove or secure these endpoints in production!
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class WebhookTestController {

    private final OrderRepository orderRepository;
    private final TeaShopBot teaShopBot;

    /**
     * Test Telegram notification for a specific order.
     * Call this AFTER checking that the order exists in DB.
     *
     * Example: GET /api/admin/test-notify/123456
     */
    @GetMapping("/test-notify/{orderCode}")
    public ResponseEntity<Map<String, Object>> testNotify(@PathVariable Long orderCode) {
        log.info("🧪 [TEST] test-notify called for order {}", orderCode);

        Order order = orderRepository.findByOrderCodeWithCustomer(orderCode).orElse(null);

        if (order == null) {
            log.warn("🧪 [TEST] Order {} not found in DB", orderCode);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Order not found",
                    "orderCode", orderCode,
                    "tip", "Make sure the order was created first via checkout"));
        }

        if (order.getCustomer() == null) {
            log.error("🧪 [TEST] Order {} has null customer!", orderCode);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Order has no customer",
                    "orderCode", orderCode,
                    "orderId", order.getId(),
                    "deliveryName", order.getDeliveryName()));
        }

        Long chatId = order.getCustomer().getTelegramChatId();
        log.info("🧪 [TEST] Order {} → customer {} → chatId {}",
                orderCode, order.getCustomer().getId(), chatId);

        try {
            teaShopBot.notifyPaymentSuccess(chatId, orderCode);
            log.info("🧪 [TEST] Telegram notification sent OK to chatId={}", chatId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderCode", orderCode,
                    "chatId", chatId,
                    "customerId", order.getCustomer().getId(),
                    "message", "Telegram notification sent! Check Telegram."));
        } catch (Exception e) {
            log.error("🧪 [TEST] Telegram notification failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "orderCode", orderCode,
                    "chatId", chatId,
                    "error", e.getMessage(),
                    "errorType", e.getClass().getSimpleName()));
        }
    }

    /**
     * List recent orders for a chatId — helps debug if orders are being created.
     *
     * Example: GET /api/admin/orders?chatId=123456789
     */
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> listOrders(
            @RequestParam Long chatId,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("🧪 [TEST] listing orders for chatId={}, limit={}", chatId, limit);

        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(chatId, limit > 50 ? 50 : limit)
                .map(orders -> {
                    var orderSummaries = orders.stream()
                            .map(o -> Map.of(
                                    "orderCode", o.getOrderCode(),
                                    "status", o.getStatus().name(),
                                    "total", o.getTotalAmount(),
                                    "deliveryName", o.getDeliveryName() != null ? o.getDeliveryName() : "null",
                                    "paymentUrl", o.getPaymentUrl() != null ? "yes" : "no"))
                            .toList();
                    return ResponseEntity.ok(Map.of(
                            "count", orders.size(),
                            "chatId", chatId,
                            "orders", orderSummaries));
                })
                .orElse(ResponseEntity.ok(Map.of(
                        "count", 0,
                        "chatId", chatId,
                        "orders", java.util.List.of()));
    }

    /**
     * Manually confirm payment for an order (bypasses payOS).
     * For testing only.
     *
     * Example: POST /api/admin/confirm-payment/123456
     */
    @PostMapping("/confirm-payment/{orderCode}")
    public ResponseEntity<Map<String, Object>> confirmPayment(@PathVariable Long orderCode) {
        log.info("🧪 [TEST] Manual confirm-payment called for order {}", orderCode);

        return orderRepository.findByOrderCodeWithCustomer(orderCode)
                .map(order -> {
                    if (order.getStatus() == com.casso.milktea.model.OrderStatus.PAID) {
                        return ResponseEntity.ok(Map.of(
                                "already", true,
                                "orderCode", orderCode,
                                "status", "PAID"));
                    }

                    order.setStatus(com.casso.milktea.model.OrderStatus.PAID);
                    orderRepository.save(order);

                    Long chatId = order.getCustomer() != null
                            ? order.getCustomer().getTelegramChatId() : null;

                    if (chatId != null) {
                        try {
                            teaShopBot.notifyPaymentSuccess(chatId, orderCode);
                        } catch (Exception e) {
                            log.error("🧪 [TEST] Telegram notify failed: {}", e.getMessage());
                        }
                    }

                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "orderCode", orderCode,
                            "status", "PAID",
                            "chatId", chatId != null ? chatId : "null"));
                })
                .orElse(ResponseEntity.ok(Map.of(
                        "error", "Order not found",
                        "orderCode", orderCode)));
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "service", "casso-milktea-bot-admin",
                "note", "Remove this controller in production!"));
    }
}
