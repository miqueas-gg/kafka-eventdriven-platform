package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.events.*;
import com.kafkaeventdriven.notification.entities.ProcessedEventEntity;
import com.kafkaeventdriven.notification.repositories.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(topics = "domain.events")
public class NotificationConsumer {

    private final NotificationDispatcher dispatcher;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaHandler
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (isInvalid(event)) return;
        
        // El dispatcher ahora se encarga de checkear duplicados y guardar auditoría
        dispatcher.dispatchOrderCreated(event);
        
        saveAndSendEnriched(event.getEventId());
    }

    @KafkaHandler
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        if (isInvalid(event)) return;
        
        dispatcher.dispatchOrderStatusChanged(event);
        
        saveAndSendEnriched(event.getEventId());
    }

    @KafkaHandler
    public void handleProductUpdated(ProductUpdatedEvent event) {
        if (isInvalid(event)) return;
        
        dispatcher.dispatchProductUpdated(event);
        
        saveAndSendEnriched(event.getEventId());
    }

    private boolean isInvalid(BaseEvent event) {
        if (event == null || event.getEventId() == null) {
            log.warn("⚠️ Recibido evento nulo o sin ID. Saltando...");
            return true;
        }
        return false;
    }

    private void saveAndSendEnriched(UUID eventId) {
        // Marcamos como procesado (Idempotencia)
        processedEventRepository.save(new ProcessedEventEntity(eventId.toString(), Instant.now()));
        
        // Enviamos el evento enriquecido protegido para que no rompa la persistencia si Kafka falla
        try {
            NotificationDispatchedEvent enriched = new NotificationDispatchedEvent();
            enriched.setEventId(eventId); 
            enriched.setOccurredAt(Instant.now());
            kafkaTemplate.send("domain.events.enriched", eventId.toString(), enriched);
        } catch (Exception e) {
            log.error("❌ Error enviando evento enriquecido a Kafka: {}", e.getMessage());
        }
    }
}