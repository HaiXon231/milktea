-- V4: Add delivery info columns to orders table
ALTER TABLE orders ADD COLUMN delivery_name VARCHAR(100);
ALTER TABLE orders ADD COLUMN delivery_phone VARCHAR(20);
ALTER TABLE orders ADD COLUMN delivery_address TEXT;
