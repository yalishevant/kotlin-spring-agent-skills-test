INSERT INTO products (id, name, category) VALUES (1, 'Classic T-Shirt', 'Apparel');
INSERT INTO products (id, name, category) VALUES (2, 'Running Shoes', 'Footwear');

INSERT INTO product_variants (id, product_id, sku, color, size, price) VALUES (1, 1, 'TSH-BLK-M', 'Black', 'M', 29.99);
INSERT INTO product_variants (id, product_id, sku, color, size, price) VALUES (2, 1, 'TSH-BLK-L', 'Black', 'L', 29.99);
INSERT INTO product_variants (id, product_id, sku, color, size, price) VALUES (3, 1, 'TSH-WHT-M', 'White', 'M', 29.99);
INSERT INTO product_variants (id, product_id, sku, color, size, price) VALUES (4, 2, 'RUN-BLU-10', 'Blue', '10', 129.99);
INSERT INTO product_variants (id, product_id, sku, color, size, price) VALUES (5, 2, 'RUN-BLU-11', 'Blue', '11', 129.99);

INSERT INTO stock_levels (id, variant_id, available_quantity, reserved_quantity, version) VALUES (1, 1, 100, 0, 0);
INSERT INTO stock_levels (id, variant_id, available_quantity, reserved_quantity, version) VALUES (2, 2, 100, 0, 0);
INSERT INTO stock_levels (id, variant_id, available_quantity, reserved_quantity, version) VALUES (3, 3, 50, 0, 0);
INSERT INTO stock_levels (id, variant_id, available_quantity, reserved_quantity, version) VALUES (4, 4, 30, 0, 0);
INSERT INTO stock_levels (id, variant_id, available_quantity, reserved_quantity, version) VALUES (5, 5, 25, 0, 0);
