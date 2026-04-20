package com.casso.milktea.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_chat_id", nullable = false, unique = true)
    private Long telegramChatId;

    @Column(length = 100)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Customer() {}

    public Customer(Long telegramChatId, String name) {
        this.telegramChatId = telegramChatId;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(Long telegramChatId) { this.telegramChatId = telegramChatId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long telegramChatId;
        private String name;
        public Builder telegramChatId(Long v) { this.telegramChatId = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Customer build() {
            Customer c = new Customer();
            c.telegramChatId = telegramChatId;
            c.name = name;
            return c;
        }
    }
}
