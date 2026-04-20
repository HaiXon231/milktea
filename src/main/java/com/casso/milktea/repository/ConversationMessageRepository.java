package com.casso.milktea.repository;

import com.casso.milktea.model.ConversationMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findTop20ByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("SELECT m FROM ConversationMessage m WHERE m.customer.id = :customerId ORDER BY m.createdAt DESC")
    List<ConversationMessage> findTopNByCustomerIdOrderByCreatedAtDesc(@Param("customerId") Long customerId, Pageable pageable);

    void deleteByCustomerId(Long customerId);
}
