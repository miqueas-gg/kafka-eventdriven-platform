-- Crear esquema si no existe
CREATE SCHEMA IF NOT EXISTS events;

-- Tabla de historial de eventos
CREATE TABLE events.events (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id       UUID NOT NULL UNIQUE, -- Para evitar duplicados (Idempotencia)
    event_type     VARCHAR(255) NOT NULL,
    source         VARCHAR(255) NOT NULL,
    payload        JSONB NOT NULL,       -- El evento completo tal cual llegó
    occurred_at    TIMESTAMPTZ NOT NULL, -- Cuándo ocurrió en el origen
    ingested_at    TIMESTAMPTZ NOT NULL DEFAULT now() -- Cuándo lo guardamos nosotros
);

-- Índices para que las consultas vuelen
CREATE INDEX idx_events_event_id ON events.events(event_id);
CREATE INDEX idx_events_event_type ON events.events(event_type);