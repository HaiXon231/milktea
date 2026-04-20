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

/**
 * Receives payOS payment webhook notifications.
 *
 * IMPORTANT PREREQUISITES:
 * 1. Webhook URL must be PUBLICLY ACCESSIBLE (not localhost).
 *    For local dev: ngrok http 8080 → set APP_BASE_URL=https://xxx.ngrok.io
 * 2. Webhook MUST be registered: WebhookInitializer calls
 *    payOS.webhooks().confirm(webhookUrl) on app startup.
 *    Without this, payOS will NOT send payment notifications!
 *
 * Webhook flow:
 * 1. App starts → WebhookInitializer registers /api/webhook/payos with payOS
 * 2. Customer pays via QR → payOS sends POST to /api/webhook/payos
 * 3. payOS verifies HMAC internally via payOS.webhooks().verify()
 * 4. Code "00" = success → update order + send Telegram notification
 */
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PayOS payOS;
    private final OrderService orderService;
    private final TeaShopBot teaShopBot;

    @PostMapping("/payos")
    public ResponseEntity<Map<String, String>> handlePayosWebhook(
            @RequestBody String webhookBody) {

        log.info("📩 [WEBHOOK] Request received, body length: {}",
                webhookBody != null ? webhookBody.length() : 0);

        if (webhookBody == null || webhookBody.isBlank()) {
            log.warn("⚠️  [WEBHOOK] Empty body — ignoring");
            return ResponseEntity.ok(Map.of("status", "IGNORED", "reason", "empty body"));
        }

        try {
            // payOS.webhooks().verify() parses JSON, checks HMAC-SHA256 signature.
            // Throws WebhookException if signature invalid.
            WebhookData data = payOS.webhooks().verify(webhookBody);

            if (data == null) {
                log.error("❌ [WEBHOOK] verify() returned null");
                return ResponseEntity.ok(Map.of("status", "ERROR", "reason", "null data"));
            }

            long orderCode = data.getOrderCode();
            String code = data.getCode();
            String desc = data.getDesc();

            log.info("📋 [WEBHOOK] order={}, code={} ({})", orderCode, code, desc);

            // Code "00" = payment SUCCESS
            if ("00".equals(code)) {
                log.info("💰 [WEBHOOK] Payment SUCCESS for order {}", orderCode);

                Order order = orderService.confirmPayment(orderCode);
                log.info("✅ [WEBHOOK] Order {} status → PAID", orderCode);

                if (order != null && order.getCustomer() != null) {
                    Long chatId = order.getCustomer().getTelegramChatId();
                    log.info("📲 [WEBHOOK] Notifying Telegram chatId={}", chatId);

                    try {
                        teaShopBot.notifyPaymentSuccess(chatId, orderCode);
                        log.info("✅ [WEBHOOK] Telegram notification sent OK");
                    } catch (Exception ex) {
                        log.error("❌ [WEBHOOK] Telegram notify failed: {}", ex.getMessage(), ex);
                    }
                } else {
                    log.error("❌ [WEBHOOK] Order or Customer is null! chatId not available. order={}",
                            orderCode);
                }
            } else {
                log.warn("⚠️  [WEBHOOK] Non-success code={} for order {} — desc={}",
                        code, orderCode, desc);
            }

            // payOS expects 200 OK — do NOT return error status or payOS retries
            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "orderCode", String.valueOf(orderCode),
                    "code", code));

        } catch (Exception e) {
            log.error("❌ [WEBHOOK] Failed: {} — {}",
                    e.getClass().getSimpleName(), e.getMessage());

            // Always return 200 so payOS doesn't retry infinitely
            return ResponseEntity.ok(Map.of(
                    "status", "ERROR",
                    "errorType", e.getClass().getSimpleName(),
                    "message", e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    /**
     * Health check — payOS calls GET /api/webhook/payos during webhook registration.
     */
    @GetMapping("/payos")
    public ResponseEntity<Map<String, String>> webhookHealthCheck() {
        log.info("📩 [WEBHOOK] Health check ping");
        return ResponseEntity.ok(Map.of(
                "status", "ALIVE",
                "service", "casso-milktea-bot"));
    }
}
