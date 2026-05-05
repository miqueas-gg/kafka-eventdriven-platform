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
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(topics = "domain.events", groupId = "notification-group")
public class NotificationConsumer {

    private final NotificationDispatcher dispatcher;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaHandler
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Convertimos UUID a String para el repositorio
        if (isDuplicate(event.getEventId().toString())) return;
        
        dispatcher.dispatchOrderCreated(event);
        
        saveAndSendEnriched(event.getEventId(), "ORDER_CREATED");
    }

    @KafkaHandler
    public void handleStatusChanged(OrderStatusChangedEvent event) {
        if (isDuplicate(event.getEventId().toString())) return;
        
        dispatcher.dispatchOrderStatusChanged(event);
        
        saveAndSendEnriched(event.getEventId(), "ORDER_STATUS_CHANGED");
    }

    @KafkaHandler
    public void handleProductUpdated(ProductUpdatedEvent event) {
        if (isDuplicate(event.getEventId().toString())) return;
        
        dispatcher.dispatchProductUpdated(event);
        
        saveAndSendEnriched(event.getEventId(), "PRODUCT_UPDATED");
    }

    private boolean isDuplicate(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

    private void saveAndSendEnriched(UUID eventId, String type) {
        processedEventRepository.save(new ProcessedEventEntity(eventId.toString(), LocalDateTime.now()));
        
        // Ajustado a los campos de NotificationDispatchedEvent
        NotificationDispatchedEvent enriched = new NotificationDispatchedEvent();
        enriched.setEventId(eventId); 
        // Si no existe setDispatchTime, prueba con setOccurredAt (que suele estar en BaseEvent)
        enriched.setOccurredAt(java.time.Instant.now());
        
        kafkaTemplate.send("domain.events.enriched", eventId.toString(), enriched);
    }
}