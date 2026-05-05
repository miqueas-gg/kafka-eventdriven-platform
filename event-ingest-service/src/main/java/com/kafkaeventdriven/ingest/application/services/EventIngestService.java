package com.kafkaeventdriven.ingest.application.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkaeventdriven.ingest.application.validator.EventValidator;
import com.kafkaeventdriven.ingest.domain.entities.EventEntity;
import com.kafkaeventdriven.ingest.domain.exceptions.InvalidEventException;
import com.kafkaeventdriven.ingest.domain.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventIngestService {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final EventValidator eventValidator; // <--- Nuevo colaborador

@Transactional
    public void processAndStore(String rawMessage) {
        try {
            // Usamos un ObjectNode para poder modificarlo si falta el ID
            JsonNode rootNode = objectMapper.readTree(rawMessage);
            if (rootNode instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
            com.fasterxml.jackson.databind.node.ObjectNode editableNode = 
                (com.fasterxml.jackson.databind.node.ObjectNode) rootNode;
            
            // --- EL ARREGLO PARA EL FRONTEND ---
            // Si el frontend no manda eventId, se lo generamos nosotros aquí
            if (!rootNode.has("eventId") || rootNode.get("eventId").isNull()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) rootNode)
                    .put("eventId", UUID.randomUUID().toString());
                log.info("EventId no proporcionado por el origen. Generado automáticamente.");
            }
            if (!editableNode.has("occurredAt") || editableNode.get("occurredAt").isNull()) {
                editableNode.put("occurredAt", java.time.Instant.now().toString());
                log.info("OccurredAt no proporcionado. Usando Instant.now().");
            }
        }
           

            // 1. VALIDACIÓN: Ahora ya no fallará por falta de ID
            eventValidator.validate(rootNode);

            // 2. EXTRACCIÓN DE ID
            UUID eventId = UUID.fromString(rootNode.get("eventId").asText());

            // 3. IDEMPOTENCIA
            if (eventRepository.existsByEventId(eventId)) {
                log.warn("Evento duplicado detectado e ignorado: {}", eventId);
                return; 
            }

            // 4. MAPEADO
            EventEntity entity = new EventEntity();
            entity.setEventId(eventId);
            entity.setEventType(rootNode.get("eventType").asText());
            entity.setSource(rootNode.get("source").asText());
            entity.setPayload(rootNode);
            entity.setOccurredAt(parseOccurredAt(rootNode.get("occurredAt")));

            // 5. PERSISTENCIA
            eventRepository.save(entity);
            log.info("Evento guardado con éxito: {} [{}]", eventId, entity.getEventType());

        } catch (InvalidEventException e) {
            log.error("Validación fallida: {}", e.getMessage());
            throw e; 
        } catch (Exception e) {
            log.error("Error técnico inesperado al procesar el evento: {}", e.getMessage());
            throw new RuntimeException("Error en la ingesta del evento", e);
        }
    }

private Instant parseOccurredAt(JsonNode dateNode) {
    // Si viene nulo o no existe, generamos el momento actual como Instant
    if (dateNode == null || dateNode.isMissingNode() || dateNode.isNull()) {
        return java.time.Instant.now();
    }

    // Si viene como número (Timestamp largo de Unix)
    if (dateNode.isNumber()) {
        return java.time.Instant.ofEpochMilli(dateNode.asLong());
    }

    // Si viene como String (ISO-8601: "2024-05-05T14:30:00Z")
    try {
        return java.time.Instant.parse(dateNode.asText());
    } catch (Exception e) {
        // Si el formato es un String simple sin zona, lo forzamos a UTC
        return java.time.LocalDateTime.parse(dateNode.asText())
                .toInstant(java.time.ZoneOffset.UTC);
    }
}
}