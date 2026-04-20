# TESTING — Test Structure & Practices

**Date:** 2026-04-20  
**Focus:** quality

## Test Files

| File | Coverage |
|------|---------|
| `src/test/java/com/casso/milktea/AiTest.java` | AI integration tests |

## Current Test State

The codebase has a single `AiTest.java` placeholder. No comprehensive test suite currently exists.

## Testing Strategy (Recommended)

### Unit Tests
- **CartService** — add/remove/update cart, validation (item exists, size, qty)
- **MenuService** — menu filtering, best seller ordering
- **OrderService** — checkout flow (empty cart, payOS failure, success)
- **AiToolFunctions** — each tool in isolation

### Integration Tests
- **GroqService** — 2-turn tool loop with mock HTTP server
- **PaymentWebhookController** — HMAC verification, order confirmation
- **TeaShopBot** — full message → response flow

### Test Patterns

```java
// Service test example
@Test
void addToCart_invalidItemId_returnsError() {
    when(menuService.findAvailableItem("INVALID")).thenReturn(Optional.empty());
    String result = cartService.addToCart(customer, "INVALID", "M", 1);
    assertTrue(result.contains("Lỗi"));
}
```

### Mock Strategy

- **Mock Repository** — use `@MockBean` for Spring context tests
- **Mock HTTP** — use `MockRestServiceServer` for Groq API tests
- **Mock Telegram** — not currently implemented

## Build & Test Commands

```bash
# Run tests
mvn test

# Skip tests during build
mvn clean package -DskipTests

# Compile only
mvn compile
```

## CI/CD

No CI pipeline currently configured. Dockerfile included for containerization.
