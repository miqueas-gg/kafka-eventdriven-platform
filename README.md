# kafka-eventdriven-platform

Plataforma de aprendizaje basada en arquitectura event-driven con Java 21, Spring Boot 3.x, Maven multi-módulo y Apache Kafka.

---

## Filosofía de trabajo

### Flujo de ramas

```
main        ← releases estables únicamente
└── develop ← integración continua del desarrollo
    ├── feature/ISSUE-1-nombre-descriptivo
    ├── feature/ISSUE-2-nombre-descriptivo
    └── ...
```

- **Se trabaja por issues.** Cada tarea tiene su issue en GitHub antes de comenzar.
- **Las releases solo van a `main`.** Nunca se hace push directo a `main`; solo se mergea desde `develop` cuando hay una versión estable.
- **`develop` es la rama base de desarrollo.** Cada issue genera su propia rama (`feature/ISSUE-X-...`) que se integra a `develop` mediante Pull Request.

### Ciclo por issue

1. Se crea la issue en GitHub con descripción y criterios de aceptación.
2. Se crea la rama `feature/ISSUE-X-descripcion` desde `develop`.
3. Se desarrolla y se abre un PR hacia `develop`.
4. El PR se revisa y, una vez aprobado, se mergea.
5. La issue se cierra automáticamente al mergear.

---

## Módulos

| Módulo | Descripción |
|---|---|
| `event-library` | Contrato de eventos compartido (`BaseEvent` y subtipos) |
| `domain-service` | API REST CRUD (productos, clientes, pedidos) + publicación a Kafka |
| `event-ingest` | Consumer: persistencia y trazabilidad de eventos |
| `notification-service` | Consumer: notificaciones asíncronas |
| `enrichment-service` | Consumer + Producer: enriquecimiento de eventos |
| `infra/` | Docker Compose con Kafka, ZooKeeper y PostgreSQL |

---

## Requisitos

| Herramienta | Versión mínima |
|---|---|
| Java (JDK) | 21 |
| Spring Boot | 3.2.x |
| Maven | 3.9.x |
| Docker Desktop | 4.x |

---

## Arranque rápido

```bash
# 1. Levantar infraestructura
cd infra && docker compose up -d

# 2. Compilar todos los módulos
mvn clean install

# 3. Arrancar un servicio
cd domain-service && mvn spring-boot:run
```
