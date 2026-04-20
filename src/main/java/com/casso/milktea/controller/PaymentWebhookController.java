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
        log.info("📩 [WEBHOOK] Received payOS webhook request");
        log.debug("🔍 [WEBHOOK] Webhook body: {}", webhookBody);

        try {
            WebhookData data = payOS.webhooks().verify(webhookBody);
            log.info("✅ [WEBHOOK] HMAC verification passed");

            long orderCode = data.getOrderCode();
            String code = data.getCode();
            log.info("📋 [WEBHOOK] order_code={}, code={}, status_description={}",
                    orderCode, code, data.getDesc());

            if ("00".equals(code)) {
                log.info("💰 [WEBHOOK] Payment confirmed! Processing order {}", orderCode);

                Order order = orderService.confirmPayment(orderCode);
                log.info("✅ [WEBHOOK] Order {} status updated to PAID", orderCode);

                if (order.getCustomer() != null) {
                    Long chatId = order.getCustomer().getTelegramChatId();
                    log.info("📲 [WEBHOOK] Sending Telegram notification to chat {}", chatId);

                    try {
                        teaShopBot.notifyPaymentSuccess(chatId, orderCode);
                        log.info("✅ [WEBHOOK] Telegram notification sent successfully");
                    } catch (Exception notifyEx) {
                        log.error("❌ [WEBHOOK] Failed to send Telegram notification: {}", notifyEx.getMessage(),
                                notifyEx);
                    }
                } else {
                    log.error("❌ [WEBHOOK] Order customer is null for order {}", orderCode);
                }
            } else {
                log.warn("⚠️ [WEBHOOK] Payment not confirmed (code={}), skipping notification", code);
            }

            log.info("✅ [WEBHOOK] Successfully processed webhook");
            return ResponseEntity.ok(Map.of("status", "OK"));

        } catch (Exception e) {
            log.error("❌ [WEBHOOK] Webhook verification/processing failed", e);
            log.error("❌ [WEBHOOK] Error type: {}, Message: {}", e.getClass().getSimpleName(), e.getMessage());

            return ResponseEntity.ok(Map.of(
                    "status", "ERROR",
                    "message", "Error: " + e.getMessage(),
                    "note", "Logged for investigation"));
        }
    }

}
