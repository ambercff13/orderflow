CREATE TABLE IF NOT EXISTS product (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         name VARCHAR(255),
                         code VARCHAR(255),
                         price NUMERIC(19, 2),
                         quantity INTEGER
);

CREATE INDEX IF NOT EXISTS idx_product_code ON product(code);