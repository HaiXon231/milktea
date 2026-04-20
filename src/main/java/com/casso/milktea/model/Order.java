package com.casso.milktea.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    @Column(columnDefinition = "TEXT")
    private String note;

    // Delivery info (collected during checkout)
    @Column(name = "delivery_name", length = 100)
    private String deliveryName;

    @Column(name = "delivery_phone", length = 20)
    private String deliveryPhone;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}

    public Order(Long orderCode, Customer customer, Integer totalAmount,
                 OrderStatus status, String note) {
        this.orderCode = orderCode;
        this.customer = customer;
        this.totalAmount = totalAmount;
        this.status = status;
        this.note = note;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = OrderStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderCode() { return orderCode; }
    public void setOrderCode(Long orderCode) { this.orderCode = orderCode; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getDeliveryName() { return deliveryName; }
    public void setDeliveryName(String deliveryName) { this.deliveryName = deliveryName; }
    public String getDeliveryPhone() { return deliveryPhone; }
    public void setDeliveryPhone(String deliveryPhone) { this.deliveryPhone = deliveryPhone; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    // Builder pattern for convenience
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Order order = new Order();
        public Builder orderCode(Long v) { order.orderCode = v; return this; }
        public Builder customer(Customer v) { order.customer = v; return this; }
        public Builder totalAmount(Integer v) { order.totalAmount = v; return this; }
        public Builder status(OrderStatus v) { order.status = v; return this; }
        public Builder note(String v) { order.note = v; return this; }
        public Builder deliveryName(String v) { order.deliveryName = v; return this; }
        public Builder deliveryPhone(String v) { order.deliveryPhone = v; return this; }
        public Builder deliveryAddress(String v) { order.deliveryAddress = v; return this; }
        public Builder paymentUrl(String v) { order.paymentUrl = v; return this; }
        public Builder items(java.util.List<OrderItem> v) { order.items = v; return this; }
        public Order build() { return order; }
    }
}