package com.casso.milktea.repository;

import com.casso.milktea.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByTelegramChatId(Long telegramChatId);
}
