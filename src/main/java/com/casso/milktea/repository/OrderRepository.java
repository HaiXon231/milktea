package com.casso.milktea.repository;

import com.casso.milktea.model.Order;
import com.casso.milktea.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(Long orderCode);
    
    @Query("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.orderCode = :orderCode")
    Optional<Order> findByOrderCodeWithCustomer(Long orderCode);
    
    List<Order> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, OrderStatus status);
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
