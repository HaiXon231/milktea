-- V3: Add sales_count column for best seller tracking
ALTER TABLE menu_item ADD COLUMN sales_count INTEGER NOT NULL DEFAULT 0;
