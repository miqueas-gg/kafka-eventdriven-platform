-- Insertar 3 Clientes con campos completos (Phone, Address JSONB, Timestamps)
INSERT INTO domain.customers (id, name, email, phone, address, created_at, updated_at) VALUES 
(gen_random_uuid(), 'Juan Pérez', 'juan.perez@email.com', '600123456', '{"street": "Calle Mayor 1", "city": "Madrid", "zip": "28001"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Maria García', 'm.garcia@email.com', '611987654', '{"street": "Av. Diagonal 45", "city": "Barcelona", "zip": "08001"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Carlos Rodríguez', 'carlos.rod@email.com', '622555444', '{"street": "Plaza Nueva 5", "city": "Sevilla", "zip": "41001"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insertar 8 Productos con campos completos (Description, Category, Status, Timestamps)
INSERT INTO  domain.products (id, name, description, price, stock, category, status, created_at, updated_at) VALUES 
(gen_random_uuid(), 'Monitor Gaming 27"', 'Monitor 4K 144Hz para gaming profesional', 299.99, 15, 'Electrónica', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Teclado Mecánico RGB', 'Teclado con switches cherry blue', 85.50, 25, 'Periféricos', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Ratón Inalámbrico', 'Sensor óptico 16000 DPI', 45.00, 40, 'Periféricos', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Auriculares Noise Cancelling', 'Cancelación de ruido activa hifi', 120.00, 10, 'Audio', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Alfombrilla XL', 'Superficie de tela de baja fricción', 19.99, 50, 'Accesorios', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Soporte Monitor Dual', 'Brazo articulado para dos pantallas', 65.00, 8, 'Mobiliario', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Webcam 1080p', 'Cámara Full HD con micrófono dual', 55.00, 20, 'Video', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Micrófono USB', 'Micrófono de condensador para podcast', 89.00, 12, 'Audio', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);