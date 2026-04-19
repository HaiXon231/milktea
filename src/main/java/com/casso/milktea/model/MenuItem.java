package com.casso.milktea.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menu_item")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @Column(name = "item_id", length = 10)
    private String itemId;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_m", nullable = false)
    private Integer priceM;

    @Column(name = "price_l", nullable = false)
    private Integer priceL;

    @Column(nullable = false)
    private Boolean available = true;

    /**
     * Returns the price based on the given size.
     */
    public int getPriceBySize(String size) {
        return "L".equalsIgnoreCase(size) ? priceL : priceM;
    }
}
