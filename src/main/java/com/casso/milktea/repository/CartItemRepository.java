package com.casso.milktea.repository;

import com.casso.milktea.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("SELECT c FROM CartItem c JOIN FETCH c.menuItem WHERE c.customer.id = :customerId")
    List<CartItem> findByCustomerIdWithMenuItem(Long customerId);

    Optional<CartItem> findByCustomerIdAndMenuItemItemIdAndSize(Long customerId, String itemId, String size);

    void deleteByCustomerId(Long customerId);

    long countByCustomerId(Long customerId);
}
