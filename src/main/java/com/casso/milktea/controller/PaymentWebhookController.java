package com.casso.milktea.controller;

import com.casso.milktea.bot.TeaShopBot;
import com.casso.milktea.model.Order;
import com.casso.milktea.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PayOS payOS;
    private final OrderService orderService;
    private final TeaShopBot teaShopBot;

    /**
     * payOS sends payment notifications here.
     * The SDK's webhooks().verify() handles HMAC-SHA256 verification.
     */
    @PostMapping("/payos")
    public ResponseEntity<Map<String, String>> handlePayosWebhook(@RequestBody Object webhookBody) {
        log.info("📩 Received payOS webhook");

        try {
            // Verify HMAC-SHA256 signature and extract data
            WebhookData data = payOS.webhooks().verify(webhookBody);

            long orderCode = data.getOrderCode();
            String code = data.getCode();

            log.info("Webhook verified - orderCode: {}, code: {}", orderCode, code);

            // code "00" means payment success
            if ("00".equals(code)) {
                Order order = orderService.confirmPayment(orderCode);
                log.info("✅ Order {} confirmed as PAID", orderCode);

                // Notify customer via Telegram
                teaShopBot.notifyPaymentSuccess(
                        order.getCustomer().getTelegramChatId(),
                        orderCode);
            }

            return ResponseEntity.ok(Map.of("status", "OK"));

        } catch (Exception e) {
            log.error("❌ Webhook verification failed", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "ERROR", "message", "Invalid webhook signature"));
        }
    }

    /**
     * Health check endpoint for payOS webhook URL verification.
     */
    @GetMapping("/payos")
    public ResponseEntity<Map<String, String>> webhookHealthCheck() {
        return ResponseEntity.ok(Map.of("status", "OK", "service", "casso-milktea-bot"));
    }
}
