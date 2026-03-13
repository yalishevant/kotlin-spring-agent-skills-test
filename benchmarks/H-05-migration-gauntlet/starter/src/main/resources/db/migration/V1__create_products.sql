CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO products (product_name, description, price, category) VALUES
    ('Widget A', 'A basic widget', 9.99, 'widgets'),
    ('Gadget B', 'An advanced gadget', 29.99, 'gadgets'),
    ('Doohickey C', 'A specialized doohickey', 49.99, 'gadgets');
