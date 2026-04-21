package com.casso.milktea.service;

import com.casso.milktea.model.MenuItem;
import com.casso.milktea.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.text.Normalizer;
import java.util.regex.Pattern;

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
                sb.append("=== ").append(currentCategory).append(" ===\n");
            }
            sb.append(String.format("• %s — M: %,dđ / L: %,dđ\n",
                    item.getName(), item.getPriceM(), item.getPriceL()));
        }
        return sb.toString();
    }

    /**
     * Format best sellers as a readable string for the AI.
     */
    public String formatBestSellersForAI(List<MenuItem> items) {
        if (items.isEmpty()) {
            return "Chưa có món bán chạy nào.";
        }
        StringBuilder sb = new StringBuilder("🔥 BEST SELLER:\n");
        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            sb.append(String.format("%d. %s — M: %,dđ / L: %,dđ (%d lượt mua)\n",
                    i + 1, item.getName(), item.getPriceM(), item.getPriceL(), item.getSalesCount()));
        }
        return sb.toString();
    }

    /**
     * Get all available best sellers (top 10 overall).
     */
    public List<MenuItem> getBestSellers(String category) {
        if (category != null && !category.isBlank()) {
            return menuItemRepository.findByCategoryAndAvailableTrueOrderBySalesCountDesc(category);
        }
        return menuItemRepository.findTop10ByAvailableTrueOrderBySalesCountDesc();
    }

    /**
     * Remove Vietnamese diacritics.
     */
    private String removeDiacritics(String str) {
        if (str == null) return null;
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD); 
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("")
                .replace('đ', 'd').replace('Đ', 'D');
    }

    /**
     * Fuzzy search by Vietnamese name.
     * Searches in itemId (exact), name (contains), category (contains).
     * Returns the best match, or empty if no match found.
     */
    public Optional<MenuItem> findByName(String query) {
        if (query == null || query.isBlank()) return Optional.empty();
        String originalQ = query.trim().toLowerCase();
        String q = removeDiacritics(originalQ);
        List<MenuItem> all = menuItemRepository.findByAvailableTrue();
        
        // Exact match on itemId first
        for (MenuItem item : all) {
            if (item.getItemId().equalsIgnoreCase(originalQ)) {
                return Optional.of(item);
            }
        }

        // Fuzzy: match each word on normalized string
        String[] words = q.split("\\s+");
        MenuItem best = null;
        double bestScore = 0;
        
        for (MenuItem item : all) {
            String nameRaw = item.getName().toLowerCase();
            String nameNormalized = removeDiacritics(nameRaw);
            String catNormalized = removeDiacritics(item.getCategory().toLowerCase());
            
            // Full string exact match (ignoring diacritics) is an instant win
            if (nameNormalized.equals(q)) {
                return Optional.of(item);
            }
            
            int matchCount = 0;
            for (String w : words) {
                if (nameNormalized.contains(w) || catNormalized.contains(w)) {
                    matchCount++;
                }
            }
            
            if (matchCount > 0) {
                // Score = (matches / total_words) - penalty for longer names
                // This favors "Trân châu đen" over "Trà sữa trân châu đen" when query is "trân châu đen"
                double score = (double) matchCount;
                // penalty: length difference
                double penalty = (double) Math.abs(nameNormalized.length() - q.length()) * 0.01;
                score = score - penalty;
                
                if (score > bestScore) {
                    bestScore = score;
                    best = item;
                }
            }
        }
        
        if (best != null && bestScore > 0) {
            return Optional.of(best);
        }
        return Optional.empty();
    }
}
