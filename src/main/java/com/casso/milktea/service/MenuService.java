package com.casso.milktea.service;

import com.casso.milktea.model.MenuItem;
import com.casso.milktea.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemRepository menuItemRepository;

    /**
     * Get all available menu items, optionally filtered by category.
     */
    public List<MenuItem> getMenu(String category) {
        if (category != null && !category.isBlank()) {
            return menuItemRepository.findByCategoryAndAvailableTrue(category);
        }
        return menuItemRepository.findByAvailableTrue();
    }

    /**
     * Find a menu item by ID and check availability.
     */
    public Optional<MenuItem> findAvailableItem(String itemId) {
        return menuItemRepository.findById(itemId)
                .filter(MenuItem::getAvailable);
    }

    /**
     * Format menu items as a readable string for the AI.
     */
    public String formatMenuForAI(List<MenuItem> items) {
        StringBuilder sb = new StringBuilder();
        String currentCategory = "";

        for (MenuItem item : items) {
            if (!item.getCategory().equals(currentCategory)) {
                currentCategory = item.getCategory();
                sb.append("\n=== ").append(currentCategory).append(" ===\n");
            }
            sb.append(String.format("• %s (%s): %s - M: %,dđ / L: %,dđ\n",
                    item.getName(), item.getItemId(), item.getDescription(),
                    item.getPriceM(), item.getPriceL()));
        }
        return sb.toString();
    }
}
