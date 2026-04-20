package com.casso.milktea.bot;

import com.casso.milktea.ai.AiChatService;
import com.casso.milktea.model.Customer;
import com.casso.milktea.service.CustomerService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeaShopBot {

    private final TelegramBot telegramBot;
    private final CustomerService customerService;
    private final AiChatService aiChatService;

    @PostConstruct
    @SuppressWarnings("deprecation")
    public void start() {
        log.info("🤖 Starting Telegram bot...");

        telegramBot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                try {
                    handleUpdate(update);
                } catch (Exception e) {
                    log.error("Error handling update", e);
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        log.info("✅ Telegram bot started and listening for messages!");
    }

    private void handleUpdate(Update update) {
        Message message = update.message();
        if (message == null)
            return;

        Long chatId = message.chat().id();
        String text = message.text();

        // Only handle text messages
        if (text == null || text.isBlank()) {
            sendMessage(chatId, "Mẹ chỉ đọc được tin nhắn chữ thôi con ơi! 😊 Gõ chữ cho mẹ nha.");
            return;
        }

        log.info("📨 Received message from chat {}: {}", chatId, text);

        // Get or create customer
        String userName = message.from() != null ? message.from().firstName() : "Khách";
        Customer customer = customerService.getOrCreateCustomer(chatId, userName);

        // Process through AI
        AiChatService.ChatResult result = aiChatService.chat(customer, text);

        // Send response
        sendMessage(chatId, result.message());

        // If there's a payment URL, send it separately with better formatting
        if (result.paymentUrl() != null) {
            String paymentMessage = "💳 Link thanh toán (QR Code):\n" + result.paymentUrl();
            sendMessage(chatId, paymentMessage);
        }
    }

    public void sendMessage(Long chatId, String text) {
        try {
            telegramBot.execute(new SendMessage(chatId, text));
        } catch (Exception e) {
            log.error("Failed to send message to chat {}", chatId, e);
        }
    }

    /**
     * Send payment success notification to a customer.
     */
    public void notifyPaymentSuccess(Long chatId, long orderCode) {
        log.info("📱 [NOTIFY] Sending payment success to chat {}, order {}", chatId, orderCode);
        try {
            String message = String.format(
                    "✅ Đã nhận thanh toán đơn hàng #%d!\n\n" +
                            "Mẹ đang làm món cho con nha. Chờ chút xíu thôi! 🧋❤️",
                    orderCode);
            sendMessage(chatId, message);
            log.info("✅ [NOTIFY] Success notification sent to chat {}", chatId);
        } catch (Exception e) {
            log.error("❌ [NOTIFY] Failed to send success notification to chat {}: {}", chatId, e.getMessage(), e);
            throw new RuntimeException("Failed to notify customer", e);
        }
    }
}
