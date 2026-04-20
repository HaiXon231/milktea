package com.casso.milktea.service;

import com.casso.milktea.model.ConversationMessage;
import com.casso.milktea.model.Customer;
import com.casso.milktea.repository.ConversationMessageRepository;
import com.casso.milktea.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ConversationMessageRepository conversationMessageRepository;

    /**
     * Find or create a customer by Telegram chat ID.
     */
    @Transactional
    public Customer getOrCreateCustomer(Long chatId, String name) {
        return customerRepository.findByTelegramChatId(chatId)
                .orElseGet(() -> {
                    Customer customer = Customer.builder()
                            .telegramChatId(chatId)
                            .name(name)
                            .build();
                    return customerRepository.save(customer);
                });
    }

    /**
     * Save a conversation message.
     */
    @Transactional
    public void saveMessage(Customer customer, String role, String content) {
        ConversationMessage message = ConversationMessage.builder()
                .customer(customer)
                .role(role)
                .content(content)
                .build();
        conversationMessageRepository.save(message);
    }

    /**
     * Get the last N messages for AI context (returned in chronological order).
     */
    public List<ConversationMessage> getRecentMessages(Customer customer) {
        return getRecentMessages(customer, 10);
    }

    public List<ConversationMessage> getRecentMessages(Customer customer, int limit) {
        List<ConversationMessage> messages =
                conversationMessageRepository.findTopNByCustomerIdOrderByCreatedAtDesc(
                        customer.getId(), Pageable.ofSize(limit));
        return messages.reversed();
    }
}
