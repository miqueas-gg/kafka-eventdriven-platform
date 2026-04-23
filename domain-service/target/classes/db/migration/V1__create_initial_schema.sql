-- =============================================
-- SCHEMA: domain
-- =============================================
CREATE SCHEMA IF NOT EXISTS domain;

-- Clientes
CREATE TABLE domain.customers (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(150)        NOT NULL,
    email        VARCHAR(255)        NOT NULL UNIQUE,
    phone        VARCHAR(20),
    address      JSONB,
    created_at   TIMESTAMPTZ         NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ         NOT NULL DEFAULT now()
);

-- Productos
CREATE TABLE domain.products (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(200)        NOT NULL,
    description  TEXT,
    price        NUMERIC(10,2)       NOT NULL CHECK (price >= 0),
    stock        INTEGER             NOT NULL DEFAULT 0 CHECK (stock >= 0),
    category     VARCHAR(100),
    status       VARCHAR(20)         NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK')),
    created_at   TIMESTAMPTZ         NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ         NOT NULL DEFAULT now()
);

-- Pedidos
CREATE TABLE domain.orders (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id    UUID            NOT NULL REFERENCES domain.customers(id),
    status         VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING','VALIDATED','PAID','CONFIRMED','CANCELLED')),
    total_amount   NUMERIC(10,2)   NOT NULL CHECK (total_amount >= 0),
    notes          TEXT,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Líneas de pedido
CREATE TABLE domain.order_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID           NOT NULL REFERENCES domain.orders(id) ON DELETE CASCADE,
    product_id   UUID           NOT NULL REFERENCES domain.products(id),
    quantity     INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price   NUMERIC(10,2)  NOT NULL CHECK (unit_price >= 0), -- precio en el momento del pedido
    subtotal     NUMERIC(10,2)  GENERATED ALWAYS AS (quantity * unit_price) STORED
);

-- Índices
CREATE INDEX ON domain.orders(customer_id);
CREATE INDEX ON domain.orders(status);
CREATE INDEX ON domain.order_items(order_id);
CREATE INDEX ON domain.order_items(product_id);