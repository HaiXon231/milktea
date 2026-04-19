-- V1: Initial schema for Milktea Bot

-- Menu items (loaded from CSV)
CREATE TABLE menu_item (
    item_id     VARCHAR(10) PRIMARY KEY,
    category    VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    price_m     INTEGER      NOT NULL,
    price_l     INTEGER      NOT NULL,
    available   BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Customers (auto-registered from Telegram)
CREATE TABLE customer (
    id               BIGSERIAL PRIMARY KEY,
    telegram_chat_id BIGINT       NOT NULL UNIQUE,
    name             VARCHAR(100),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Shopping cart (per customer, cleared after checkout)
CREATE TABLE cart_item (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT      NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    item_id     VARCHAR(10) NOT NULL REFERENCES menu_item(item_id),
    size        VARCHAR(1)  NOT NULL CHECK (size IN ('M', 'L')),
    quantity    INTEGER     NOT NULL CHECK (quantity > 0),
    unit_price  INTEGER     NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (customer_id, item_id, size)
);

-- Orders
CREATE TABLE orders (
    id           BIGSERIAL PRIMARY KEY,
    order_code   BIGINT       NOT NULL UNIQUE,
    customer_id  BIGINT       NOT NULL REFERENCES customer(id),
    total_amount INTEGER      NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    payment_url  TEXT,
    note         TEXT,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Order line items (snapshot of cart at checkout time)
CREATE TABLE order_item (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    item_id    VARCHAR(10)  NOT NULL,
    item_name  VARCHAR(100) NOT NULL,
    size       VARCHAR(1)   NOT NULL,
    quantity   INTEGER      NOT NULL,
    unit_price INTEGER      NOT NULL
);

-- Conversation history (for AI context window)
CREATE TABLE conversation_message (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT      NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL,
    content     TEXT        NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_cart_customer ON cart_item(customer_id);
CREATE INDEX idx_order_customer ON orders(customer_id);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_conversation_customer ON conversation_message(customer_id, created_at);
