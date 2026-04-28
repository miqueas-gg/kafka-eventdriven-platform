package com.kafkaeventdriven.ingest.application.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkaeventdriven.ingest.domain.entities.EventEntity;
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
    private final ObjectMapper objectMapper; // El traductor de JSON de Spring

    @Transactional
    public void processAndStore(String rawMessage) {
        try {
            // 1. Convertimos el texto bruto en un objeto JSON (JsonNode)
            JsonNode root = objectMapper.readTree(rawMessage);
            
            JsonNode eventIdNode = root.get("eventId");
            if (eventIdNode == null || eventIdNode.isNull()) {
                log.error("Saltando mensaje: No tiene eventId");
                return; // Ignoramos mensajes basura sin ID
            }
            // 2. Extraemos el eventId para la Idempotencia
            UUID eventId = UUID.fromString(root.get("eventId").asText());

            // 3. REQUISITO: Comprobar si ya existe (Idempotencia)
            if (eventRepository.existsByEventId(eventId)) {
                log.warn("Evento duplicado detectado e ignorado: {}", eventId);
                return;
            }

            // 4. Mapeamos los datos del JSON a nuestra entidad de Base de Datos
            EventEntity entity = new EventEntity();
            entity.setEventId(eventId);
            entity.setEventType(root.get("eventType").asText());
            entity.setSource(root.has("source") && !root.get("source").isNull() 
                             ? root.get("source").asText() : "unknown-source");
            entity.setPayload(root); // Guardamos el JSON completo en la columna JSONB
            JsonNode dateNode = root.get("occurredAt");
            if (dateNode.isNumber()) {
                // Si es un timestamp numérico (como el de tus logs)
                long seconds = dateNode.asLong();
                entity.setOccurredAt(java.time.Instant.ofEpochSecond(seconds)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            } else {
                // Si es un String ISO normal
                entity.setOccurredAt(LocalDateTime.parse(dateNode.asText()));
            }

            // 5. Guardamos en la tabla eventstore.events
            eventRepository.save(entity);
            log.info("Evento guardado con éxito: {} [{}]", eventId, entity.getEventType());

        } catch (Exception e) {
            log.error("Error al procesar el evento: {}", e.getMessage());
            // Lanzamos excepción para que Kafka sepa que ha fallado y reintente
            throw new RuntimeException("Error en la ingesta del evento", e);
        }
    }
}