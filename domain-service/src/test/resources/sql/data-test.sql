-- Limpiar en orden de dependencia (primero la tabla hija)
DELETE FROM domain.order_items;
DELETE FROM domain.orders;
DELETE FROM domain.products;
DELETE FROM domain.customers;

-- Insertar Customer (con address como JSONB)
INSERT INTO domain.customers (id, name, email, phone, address, created_at, updated_at) 
VALUES (
    '550e8400-e29b-41d4-a716-446655440000', 
    'Juan Pérez', 
    'test@test.com', 
    '600123456', 
    '{"street": "Calle Falsa 123", "city": "Madrid"}'::jsonb, 
    NOW(), 
    NOW()
);

-- Insertar Product (con status y category)
INSERT INTO domain.products (id, name, description, price, stock, category, status, created_at, updated_at) 
VALUES (
    '770e8400-e29b-41d4-a716-446655441111', 
    'Producto Test', 
    'Descripción de prueba', 
    25.50, 
    100, 
    'General', 
    'ACTIVE', -- Ajusta según tu Enum (ACTIVE/IN_ACTIVE)
    NOW(), 
    NOW()
);