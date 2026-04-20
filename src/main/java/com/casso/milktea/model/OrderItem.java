package com.casso.milktea.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "item_id", nullable = false, length = 10)
    private String itemId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(nullable = false, length = 1)
    private String size;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    public OrderItem() {}

    public OrderItem(Order order, String itemId, String itemName, String size,
                      Integer quantity, Integer unitPrice) {
        this.order = order;
        this.itemId = itemId;
        this.itemName = itemName;
        this.size = size;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Integer unitPrice) { this.unitPrice = unitPrice; }
    public int getSubtotal() { return unitPrice * quantity; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Order order;
        private String itemId;
        private String itemName;
        private String size;
        private Integer quantity;
        private Integer unitPrice;
        public Builder order(Order v) { this.order = v; return this; }
        public Builder itemId(String v) { this.itemId = v; return this; }
        public Builder itemName(String v) { this.itemName = v; return this; }
        public Builder size(String v) { this.size = v; return this; }
        public Builder quantity(Integer v) { this.quantity = v; return this; }
        public Builder unitPrice(Integer v) { this.unitPrice = v; return this; }
        public OrderItem build() {
            OrderItem i = new OrderItem(order, itemId, itemName, size, quantity, unitPrice);
            return i;
        }
    }
}
