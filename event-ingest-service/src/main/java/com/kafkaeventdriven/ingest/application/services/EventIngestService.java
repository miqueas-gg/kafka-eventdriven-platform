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

import java.time.LocalDateTime;
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
            JsonNode root = objectMapper.readTree(rawMessage);

            // 1. VALIDACIÓN: Si falla, lanza InvalidEventException
            eventValidator.validate(root);

            // 2. EXTRACCIÓN DE ID (Ya sabemos que existe por el validador)
            UUID eventId = UUID.fromString(root.get("eventId").asText());

            // 3. IDEMPOTENCIA: Si es duplicado, descartamos silenciosamente (WARN)
            if (eventRepository.existsByEventId(eventId)) {
                log.warn("Evento duplicado detectado e ignorado: {}", eventId);
                return; 
            }

            // 4. MAPEADO (Mucho más limpio ahora que el validador filtró la basura)
            EventEntity entity = new EventEntity();
            entity.setEventId(eventId);
            entity.setEventType(root.get("eventType").asText());
            entity.setSource(root.get("source").asText());
            entity.setPayload(root);
            
            // Reutilizamos la lógica de fecha flexible por si acaso
            entity.setOccurredAt(parseOccurredAt(root.get("occurredAt")));

            // 5. PERSISTENCIA
            eventRepository.save(entity);
            log.info("Evento guardado con éxito: {} [{}]", eventId, entity.getEventType());

        } catch (InvalidEventException e) {
            log.error("Validación fallida: {}", e.getMessage());
            throw e; // La relanzamos para que el Consumer la gestione hacia el DLT
        } catch (Exception e) {
            log.error("Error técnico inesperado al procesar el evento: {}", e.getMessage());
            throw new RuntimeException("Error en la ingesta del evento", e);
        }
    }

    private LocalDateTime parseOccurredAt(JsonNode dateNode) {
        if (dateNode.isNumber()) {
            return java.time.Instant.ofEpochSecond(dateNode.asLong())
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        }
        return LocalDateTime.parse(dateNode.asText());
    }
}