package com.casso.milktea.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_item",
       uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "item_id", "size"}))
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false, length = 1)
    private String size;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public CartItem() {}

    public CartItem(Customer customer, MenuItem menuItem, String size, Integer quantity, Integer unitPrice) {
        this.customer = customer;
        this.menuItem = menuItem;
        this.size = size;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public MenuItem getMenuItem() { return menuItem; }
    public void setMenuItem(MenuItem menuItem) { this.menuItem = menuItem; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Integer unitPrice) { this.unitPrice = unitPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getSubtotal() { return unitPrice * quantity; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Customer customer;
        private MenuItem menuItem;
        private String size;
        private Integer quantity;
        private Integer unitPrice;
        public Builder customer(Customer v) { this.customer = v; return this; }
        public Builder menuItem(MenuItem v) { this.menuItem = v; return this; }
        public Builder size(String v) { this.size = v; return this; }
        public Builder quantity(Integer v) { this.quantity = v; return this; }
        public Builder unitPrice(Integer v) { this.unitPrice = v; return this; }
        public CartItem build() {
            CartItem c = new CartItem();
            c.customer = customer;
            c.menuItem = menuItem;
            c.size = size;
            c.quantity = quantity;
            c.unitPrice = unitPrice;
            return c;
        }
    }
}
