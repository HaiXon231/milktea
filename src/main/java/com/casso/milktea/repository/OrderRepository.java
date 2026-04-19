package com.casso.milktea.repository;

import com.casso.milktea.model.Order;
import com.casso.milktea.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(Long orderCode);
    List<Order> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, OrderStatus status);
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
