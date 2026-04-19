package com.casso.milktea.repository;

import com.casso.milktea.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, String> {
    List<MenuItem> findByCategoryAndAvailableTrue(String category);
    List<MenuItem> findByAvailableTrue();
}
