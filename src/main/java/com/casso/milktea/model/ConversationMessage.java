package com.casso.milktea.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_message")
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 20)
    private String role; // "user", "assistant", "system"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public ConversationMessage() {}

    public ConversationMessage(Customer customer, String role, String content) {
        this.customer = customer;
        this.role = role;
        this.content = content;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Customer customer;
        private String role;
        private String content;
        public Builder customer(Customer v) { this.customer = v; return this; }
        public Builder role(String v) { this.role = v; return this; }
        public Builder content(String v) { this.content = v; return this; }
        public ConversationMessage build() {
            ConversationMessage m = new ConversationMessage();
            m.customer = customer;
            m.role = role;
            m.content = content;
            return m;
        }
    }
}