package com.casso.milktea.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;
import vn.payos.model.webhooks.ConfirmWebhookResponse;

/**
 * Registers the payOS webhook URL on application startup.
 *
 * payOS requires you to CONFIRM your webhook URL before it sends
 * payment notifications to that URL. This component calls:
 *   payOS.webhooks().confirm(webhookUrl)
 * once on startup so payOS knows where to send payment notifications.
 *
 * The webhook URL must be publicly accessible (not localhost).
 * For local dev, use ngrok: ngrok http 8080
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookInitializer {

    private final PayOS payOS;

    @Value("${app.base-url}")
    private String baseUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhook() {
        String webhookUrl = baseUrl + "/api/webhook/payos";
        log.info("🔗 [WEBHOOK-INIT] Registering payOS webhook URL: {}", webhookUrl);

        try {
            ConfirmWebhookResponse response = payOS.webhooks().confirm(webhookUrl);
            log.info("✅ [WEBHOOK-INIT] Webhook registered successfully! Response: {}",
                    response);
        } catch (Exception e) {
            log.error("❌ [WEBHOOK-INIT] Failed to register webhook: {}. "
                    + "Payment notifications will NOT be received! "
                    + "Make sure the webhook URL is publicly accessible.", e.getMessage());
            log.error("   💡 For local dev: use 'ngrok http 8080' and set APP_BASE_URL=https://xxx.ngrok.io");
        }
    }
}