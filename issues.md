# Issues – kafka-eventdriven-platform

Listado de issues planificadas para el proyecto. Una vez acordadas, se crearán en GitHub.

---

## Infraestructura

- [ ] **[INFRA-1]** Crear `infra/docker-compose.yml` con Kafka, ZooKeeper y PostgreSQL
- [ ] **[INFRA-2]** Verificar conectividad entre servicios en entorno local (healthchecks)

---

## Fase 1 – Estructura base y flujo inicial

### `event-library`
- [ ] **[LIB-1]** Crear módulo Maven `event-library` con `BaseEvent` abstracto
- [ ] **[LIB-2]** Implementar `OrderCreatedEvent`, `OrderStatusChangedEvent`, `ProductUpdatedEvent`
- [ ] **[LIB-3]** Implementar `NotificationDispatchedEvent` y `EventEnrichedEvent`
- [ ] **[LIB-4]** Configurar serialización/deserialización JSON (Jackson)

### `domain-service`
- [ ] **[DOM-1]** Crear módulo Spring Boot `domain-service` con dependencia a `event-library`
- [ ] **[DOM-2]** Configurar Flyway y crear schema `domain` (customers, products, orders, order_items)
- [ ] **[DOM-3]** Implementar CRUD de `customers`
- [ ] **[DOM-4]** Implementar CRUD de `products`
- [ ] **[DOM-5]** Implementar CRUD de `orders` con sus `order_items`
- [ ] **[DOM-6]** Publicar `OrderCreatedEvent` a Kafka al crear pedido (fire and forget)
- [ ] **[DOM-7]** Publicar `OrderStatusChangedEvent` al cambiar estado de pedido
- [ ] **[DOM-8]** Publicar `ProductUpdatedEvent` al modificar precio/stock/estado

---

## Fase 2 – Consistencia y Pipeline de Eventos

### `domain-service`
- [ ] **[DOM-9]** Reemplazar fire-and-forget por `@Transactional` + `kafkaTemplate.send().get()`

### `event-ingest`
- [ ] **[INGEST-1]** Crear módulo Spring Boot `event-ingest`
- [ ] **[INGEST-2]** Configurar Flyway y crear schema `events` (events, dead_letter)
- [ ] **[INGEST-3]** Implementar consumer de `domain.events` que persiste en `events.events`
- [ ] **[INGEST-4]** Gestionar idempotencia por `eventId`
- [ ] **[INGEST-5]** Configurar Dead Letter Topic (DLT) con reintento x3

### `notification-service`
- [ ] **[NOTIF-1]** Crear módulo Spring Boot `notification-service`
- [ ] **[NOTIF-2]** Configurar tabla `events.notifications`
- [ ] **[NOTIF-3]** Implementar consumer de `domain.events` con log/mock de notificación
- [ ] **[NOTIF-4]** Configurar retry con backoff exponencial
- [ ] **[NOTIF-5]** Persistir resultado en `events.notifications`

### `enrichment-service`
- [ ] **[ENRICH-1]** Crear módulo Spring Boot `enrichment-service`
- [ ] **[ENRICH-2]** Configurar tabla `events.event_enrichments`
- [ ] **[ENRICH-3]** Implementar consumer de `domain.events`
- [ ] **[ENRICH-4]** Enriquecer eventos con datos actuales de la DB de dominio
- [ ] **[ENRICH-5]** Publicar `EventEnrichedEvent` a `domain.events.enriched`
- [ ] **[ENRICH-6]** Persistir enriquecimiento en `events.event_enrichments`

---

## Fase 3 – Stretch Goals

### Outbox Pattern
- [ ] **[OUTBOX-1]** Crear tabla `domain.outbox` en `domain-service`
- [ ] **[OUTBOX-2]** Modificar flujo de creación para escribir en outbox en vez de publicar directo
- [ ] **[OUTBOX-3]** Implementar poller `@Scheduled` que lee outbox y publica a Kafka

### `monitoring-service`
- [ ] **[MON-1]** Crear módulo `monitoring-service` con métricas de eventos procesados
- [ ] **[MON-2]** Consumir de `domain.events.enriched` y exponer métricas

### `anomaly-detector`
- [ ] **[ANOM-1]** Crear módulo `anomaly-detector` con reglas básicas de detección
- [ ] **[ANOM-2]** Alertar cuando un pedido supere umbral de importe o se cancele repetidamente
