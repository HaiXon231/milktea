package com.casso.milktea.ai;

import com.casso.milktea.model.ConversationMessage;
import com.casso.milktea.model.Customer;
import com.casso.milktea.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade — delegates to GroqService which has full control over the
 * two-turn Groq → function → Groq loop.
 *
 * AiChatService is kept as the entry point so other code
 * (TeaShopBot) doesn't need to change.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final GroqService groqService;
    private final CustomerService customerService;

    public ChatResult chat(Customer customer, String userMessage) {
        customerService.saveMessage(customer, "user", userMessage);

        List<ConversationMessage> history = customerService.getRecentMessages(customer, 8);

        GroqService.ChatResponse response = groqService.chat(customer, userMessage, history);

        if (response.message() == null || response.message().isBlank()) {
            response = new GroqService.ChatResponse(
                    "Mẹ nghe con nói rồi, con nhắn lại giúp mẹ nha 😊",
                    null, null);
        }

        customerService.saveMessage(customer, "assistant", response.message());

        return new ChatResult(response.message(), response.paymentUrl(), response.orderCode());
    }

    public record ChatResult(String message, String paymentUrl, Long orderCode) {}
}