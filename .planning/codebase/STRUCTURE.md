# STRUCTURE — Directory Layout & Key Files

**Date:** 2026-04-20  
**Focus:** arch

## Directory Layout

```
src/main/java/com/casso/milktea/
├── MilkteaBotApplication.java          # Spring Boot entry point
├── ai/
│   ├── AiChatService.java              # Facade: calls GroqService, saves history
│   ├── GroqService.java               # Direct Groq HTTP API with 2-turn tool loop
│   └── AiToolFunctions.java            # 7 tool implementations (menu, cart, checkout, best sellers)
├── bot/
│   └── TeaShopBot.java                # Telegram Long Polling listener + sender
├── config/
│   ├── SpringAiConfig.java             # RestTemplate bean (with timeouts)
│   ├── TelegramBotConfig.java         # TelegramBot bean
│   ├── PayOSConfig.java                # PayOS client bean
│   └── PlainToolCallResultConverter.java  # Strip \n from tool results for Groq
├── controller/
│   ├── PaymentWebhookController.java  # payOS webhook → confirm order
│   └── PaymentRedirectController.java # Success/cancel HTML pages
├── model/
│   ├── Customer.java                  # Entity: telegram auto-registration
│   ├── ConversationMessage.java       # Entity: AI conversation history
│   ├── MenuItem.java                  # Entity: menu with sales_count
│   ├── CartItem.java                  # Entity: per-customer cart
│   ├── Order.java                     # Entity: checkout snapshot
│   ├── OrderItem.java                 # Entity: line items (frozen price)
│   └── OrderStatus.java               # Enum: PENDING → PAID → COMPLETED
├── repository/
│   ├── CustomerRepository.java
│   ├── ConversationMessageRepository.java
│   ├── MenuItemRepository.java         # Best seller queries
│   ├── CartItemRepository.java
│   └── OrderRepository.java
└── service/
    ├── CustomerService.java           # Auto-register, save/view history
    ├── MenuService.java                # Menu queries + format for AI
    ├── CartService.java                # Cart CRUD + validation
    └── OrderService.java               # Checkout → payOS → payment link

src/main/resources/
├── application.yml                     # All config (DB, AI, Telegram, payOS)
└── db/migration/
    ├── V1__init_schema.sql             # Create all tables
    ├── V2__seed_menu_data.sql         # Seed menu from Menu.csv
    └── V3__add_sales_count.sql        # Add sales_count column

src/test/java/
└── AiTest.java                        # Unit test for AI integration

./
├── pom.xml                            # Maven config + dependencies
├── Dockerfile                          # Multi-stage Docker build
├── Menu.csv                            # Menu seed data
├── flow.txt                            # Conversation + payment flow spec
├── CLAUDE.md                           # Claude Code project guidance
└── .vscode/launch.json               # Spring Boot run with .env auto-load
```

## Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Packages | lowercase | `com.casso.milktea.ai` |
| Java classes | PascalCase | `GroqService.java` |
| DB tables | snake_case | `menu_item`, `order_item` |
| DB columns | snake_case | `telegram_chat_id`, `sales_count` |
| Java fields | camelCase | `telegramChatId`, `salesCount` |
| API tools | snake_case | `get_menu`, `add_to_cart` |

## Key File Locations

| Purpose | File |
|---------|------|
| AI orchestration | `ai/GroqService.java`, `ai/AiChatService.java` |
| Tool definitions | `ai/AiToolFunctions.java` |
| System prompt | `GroqService.systemPrompt()` |
| Telegram handler | `bot/TeaShopBot.java` |
| Payment webhook | `controller/PaymentWebhookController.java` |
| Cart logic | `service/CartService.java` |
| Order + payOS | `service/OrderService.java` |
| DB migrations | `src/main/resources/db/migration/V*.sql` |
