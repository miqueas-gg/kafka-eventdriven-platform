package com.kafkaeventdriven.ingest.infrastructure.rest;

import com.kafkaeventdriven.ingest.domain.entities.EventEntity;
import com.kafkaeventdriven.ingest.domain.repositories.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
  // 1. La URL ahora lleva el comando para crear el esquema 'events' al arrancar
    "spring.datasource.url=jdbc:h2:mem:querytestdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS events",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    
    // 2. Flyway fuera (usa SQL de Postgres que H2 no entiende)
    "spring.flyway.enabled=false",
    
    // 3. Hibernate crea las tablas solo para el test
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    
    // 4. Importante: Forzamos a Hibernate a usar el esquema que acabamos de crear
    "spring.jpa.properties.hibernate.default_schema=events",
    
    // 5. Kafka apagado (no es necesario para probar la API de consulta)
    "spring.kafka.listener.auto-startup=false"
})
class EventQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        
        // Insertamos datos de prueba usando Instant
        saveEvent("ORDER_CREATED", "sales-service", Instant.now().minus(2, ChronoUnit.DAYS));
        saveEvent("ORDER_CREATED", "sales-service", Instant.now().minus(1, ChronoUnit.DAYS));
        saveEvent("USER_LOGGED_IN", "auth-service", Instant.now());
    }

    @Test
    void shouldListAllEventsPaginanted() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void shouldFilterByEventType() throws Exception {
        mockMvc.perform(get("/api/events").param("eventType", "USER_LOGGED_IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].eventType").value("USER_LOGGED_IN"));
    }

    @Test
    void shouldReturnStats() throws Exception {
        mockMvc.perform(get("/api/events/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))); // ORDER_CREATED y USER_LOGGED_IN
    }

    @Test
    void shouldFindByIdInternal() throws Exception {
        EventEntity saved = eventRepository.findAll().get(0);
        
        mockMvc.perform(get("/api/events/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()));
    }

    private void saveEvent(String type, String source, Instant occurredAt) {
        EventEntity entity = new EventEntity();
        entity.setEventId(UUID.randomUUID());
        entity.setEventType(type);
        entity.setSource(source);
        entity.setPayload(objectMapper.createObjectNode().put("test", "data"));
        entity.setOccurredAt(occurredAt);
        eventRepository.save(entity);
    }
}