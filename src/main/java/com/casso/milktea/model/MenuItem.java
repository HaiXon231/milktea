package com.casso.milktea.model;

import jakarta.persistence.*;

@Entity
@Table(name = "menu_item")
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

    @Column(name = "sales_count", nullable = false)
    private Integer salesCount = 0;

    public MenuItem() {}

    public MenuItem(String itemId, String category, String name, String description,
                    Integer priceM, Integer priceL, Boolean available, Integer salesCount) {
        this.itemId = itemId;
        this.category = category;
        this.name = name;
        this.description = description;
        this.priceM = priceM;
        this.priceL = priceL;
        this.available = available;
        this.salesCount = salesCount;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPriceM() { return priceM; }
    public void setPriceM(Integer priceM) { this.priceM = priceM; }
    public Integer getPriceL() { return priceL; }
    public void setPriceL(Integer priceL) { this.priceL = priceL; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    public Integer getSalesCount() { return salesCount; }
    public void setSalesCount(Integer salesCount) { this.salesCount = salesCount; }

    public int getPriceBySize(String size) {
        return "L".equalsIgnoreCase(size) ? priceL : priceM;
    }
}
