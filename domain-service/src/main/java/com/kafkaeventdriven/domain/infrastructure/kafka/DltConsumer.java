package com.kafkaeventdriven.domain.infrastructure.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkaeventdriven.domain.entities.DeadLetterEvent;
import com.kafkaeventdriven.domain.repositories.DeadLetterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DltConsumer {

    private final DeadLetterRepository deadLetterRepository;
    private final ObjectMapper objectMapper; // Para cotillear dentro del JSON del evento

    @KafkaListener(topics = "domain.events.DLT", groupId = "domain-service-dlt")
    public void handleDlt(
            String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = "x-exception-message", defaultValue = "Error desconocido") String errorMessage,
            @Header(name = "x-exception-stacktrace", defaultValue = "No stacktrace") String stacktrace
    ) {
        log.info("Recogiendo evento fallido del DLT: {}", message);

        try {
            // Leemos el mensaje para sacar el ID y el tipo de evento original
            JsonNode json = objectMapper.readTree(message);
            
            // Sacamos el correlationId o el aggregateId para saber qué evento era
            String eventIdStr = json.has("correlationId") ? json.get("correlationId").asText() : UUID.randomUUID().toString();
            String eventType = json.has("eventType") ? json.get("eventType").asText() : "UNKNOWN";

            DeadLetterEvent dltEvent = DeadLetterEvent.builder()
                    .originalTopic("domain.events") // El topic de donde venía
                    .eventId(UUID.fromString(eventIdStr))
                    .eventType(eventType)
                    .payload(message) // El JSON completo para poder reintentarlo luego
                    .errorMessage(errorMessage)
                    .failedAt(Instant.now())
                    .retryCount(1) // Empezamos la cuenta
                    .build();

            deadLetterRepository.save(dltEvent);
            log.info("Evento fallido persistido en base de datos. ID interno: {}", dltEvent.getId());
            
        } catch (Exception e) {
            log.error("¡Cuidado! No hemos podido ni siquiera guardar el error en el DLT: {}", e.getMessage());
        }
    }
}