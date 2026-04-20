# CONVENTIONS — Code Style & Patterns

**Date:** 2026-04-20  
**Focus:** quality

## Java Style

- **No Lombok annotations** — all models use hand-written getters/setters/constructors/builders
- **Records** for tool request DTOs: `public record MenuReq() {}`
- **Sealed classes / switch expressions** for tool dispatch: `return switch (toolName) { ... }`
- **`@RequiredArgsConstructor` on services** — Spring DI for service beans (not models)
- **`@Slf4j`** on service/AI classes for structured logging

## AI Tool Design

### Tool Request Records (input schemas)

```java
public record MenuReq() {}
public record AddCartReq(String itemId, String size, Integer quantity) {}
public record ViewCartReq() {}
public record UpdateCartReq(String itemId, String size, Integer quantity) {}
public record RemoveCartReq(String itemId, String size) {}
public record CheckoutReq(String note) {}
public record BestSellerReq(String category) {}
```

### Tool Result Contract

Every tool function returns a `String`. `checkout()` returns `OrderService.OrderResult` which has `toToolResult()`.

### Error Handling in Tools

All tool methods catch exceptions and return descriptive error strings — these are fed back to Groq so it can self-correct.

```java
public String getMenu(String category) {
    try {
        List<MenuItem> items = menuService.getMenu(category);
        return menuService.formatMenuForAI(items);
    } catch (Exception e) {
        log.error("getMenu failed", e);
        return "Lỗi khi lấy menu: " + e.getMessage();
    }
}
```

## Groq Service Patterns

### 2-Turn Tool Loop

```java
// Turn 1
JsonNode response = callGroq(messages, true);
JsonNode toolCalls = response.path("choices").get(0).path("message").path("tool_calls");

if (toolCalls.isArray() && !toolCalls.isEmpty()) {
    for (JsonNode tc : toolCalls) {
        String result = executeTool(toolName, args, customer);
        messages.add(buildToolMessage(toolCallId, toolName, result));
    }
    // Turn 2
    response = callGroq(messages, false);
}
```

### System Prompt Pattern

Plain ASCII text (no accents) to minimize token count and parsing issues:
```java
"Ban la Me - ba chu quan tra sua, noi chuyen nhieu, goi khach la \"con\". ..."
```

## Model Patterns

### Builder (replaces Lombok `@Builder`)

```java
public static Builder builder() { return new Builder(); }
public static class Builder {
    private Order order = new Order();
    public Builder customer(Customer v) { order.customer = v; return this; }
    public Order build() { return order; }
}
```

### Timestamps via `@PrePersist`

```java
@PrePersist
protected void onCreate() {
    this.createdAt = LocalDateTime.now();
}
```

## Error Handling Pattern

Groq errors → descriptive user-facing messages (not stack traces):
```java
private String errorMessage(Exception e) {
    if (msg.contains("timeout")) return "Mang hoi cham con oi...";
    if (msg.contains("rate limit")) return "Me dang ban lac...";
    // ...
}
```

## Database Conventions

- **DDL auto:** `validate` — no Hibernate auto-DDL, use Flyway only
- **Lazy loading** for `@ManyToOne` relationships
- **CascadeType.ALL + orphanRemoval** on `Order.items`
- **Snapshot pattern** for `order_item` — stores `itemId`, `itemName`, `unitPrice` directly (not FK)
