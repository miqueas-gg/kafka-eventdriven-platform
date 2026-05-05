package com.kafkaeventdriven.ingest.application.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.kafkaeventdriven.ingest.domain.exceptions.InvalidEventException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class EventValidator {

    public void validate(JsonNode root) {
        // 1. eventId no nulo
        if (root.get("eventId") == null || root.get("eventId").isNull()) {
            throw new InvalidEventException("El campo 'eventId' es obligatorio");
        }

        // 2. eventType no nulo ni vacío
        if (root.get("eventType") == null || root.get("eventType").asText().trim().isEmpty()) {
            throw new InvalidEventException("El campo 'eventType' es obligatorio y no puede estar vacío");
        }

        // 3. source no nulo ni vacío
        if (root.get("source") == null || root.get("source").isNull() || root.get("source").asText().trim().isEmpty()) {
            throw new InvalidEventException("El campo 'source' es obligatorio");
        }

        // 4. version presente
        if (root.get("version") == null || root.get("version").isNull()) {
            throw new InvalidEventException("El campo 'version' es obligatorio");
        }

        // 5. occurredAt no nulo y no en el futuro (tolerancia 5 min)
        validateDate(root.get("occurredAt"));
    }

   private void validateDate(JsonNode dateNode) {
        if (dateNode == null || dateNode.isNull()) {
            throw new InvalidEventException("El campo 'occurredAt' es obligatorio");
        }

        try {
            java.time.Instant occurredAt;
            
            if (dateNode.isNumber()) {
                // Soporta tanto segundos como milisegundos (detección automática simple)
                long value = dateNode.asLong();
                if (value > 9999999999L) { // Probablemente milisegundos
                    occurredAt = java.time.Instant.ofEpochMilli(value);
                } else { // Probablemente segundos
                    occurredAt = java.time.Instant.ofEpochSecond(value);
                }
            } else {
                // Intenta parsear como Instant (formato ISO-8601 con Z)
                // Si falla, intenta parsear como LocalDateTime y convertirlo
                try {
                    occurredAt = java.time.Instant.parse(dateNode.asText());
                } catch (DateTimeParseException e) {
                    occurredAt = LocalDateTime.parse(dateNode.asText())
                            .atZone(java.time.ZoneId.systemDefault()).toInstant();
                }
            }

            // Validación de futuro con margen de 5 minutos usando Instant
            java.time.Instant limit = java.time.Instant.now().plus(5, java.time.temporal.ChronoUnit.MINUTES);
            if (occurredAt.isAfter(limit)) {
                throw new InvalidEventException("La fecha 'occurredAt' no puede estar en el futuro");
            }

        } catch (Exception e) {
            log.error("Error al validar fecha: {}", e.getMessage());
            throw new InvalidEventException("Formato de fecha 'occurredAt' inválido");
        }
    }
}