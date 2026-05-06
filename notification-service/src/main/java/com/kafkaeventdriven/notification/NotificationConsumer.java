package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.events.*;
import com.kafkaeventdriven.notification.entities.NotificationEntity;
import com.kafkaeventdriven.notification.entities.ProcessedEventEntity;
import com.kafkaeventdriven.notification.repositories.NotificationRepository;
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
    private final NotificationRepository notificationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaHandler
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (isInvalid(event)) return;
        if (isDuplicate(event.getEventId().toString())) return;
        
        try {
            dispatcher.dispatchOrderCreated(event);
            saveAudit(event.getEventId(), "ORDER_CREATED", event.getCustomerEmail(), "SENT");
        } catch (Exception e) {
            saveAudit(event.getEventId(), "ORDER_CREATED", event.getCustomerEmail(), "FAILED");
            throw e; 
        }
        saveAndSendEnriched(event.getEventId(), "ORDER_CREATED");
    }

    @KafkaHandler
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        if (isInvalid(event)) return;
        if (isDuplicate(event.getEventId().toString())) return;
        
        try {
            dispatcher.dispatchOrderStatusChanged(event);
            // Aquí usamos un recipient genérico o del evento si lo tuviera
            saveAudit(event.getEventId(), "ORDER_STATUS_CHANGED", "customer@example.com", "SENT");
        } catch (Exception e) {
            saveAudit(event.getEventId(), "ORDER_STATUS_CHANGED", "customer@example.com", "FAILED");
            throw e;
        }
        saveAndSendEnriched(event.getEventId(), "ORDER_STATUS_CHANGED");
    }

    @KafkaHandler
    public void handleProductUpdated(ProductUpdatedEvent event) {
        if (isInvalid(event)) return;
        if (isDuplicate(event.getEventId().toString())) return;
        
        try {
            dispatcher.dispatchProductUpdated(event);
            saveAudit(event.getEventId(), "PRODUCT_UPDATED", "admin@system.com", "SENT");
        } catch (Exception e) {
            saveAudit(event.getEventId(), "PRODUCT_UPDATED", "admin@system.com", "FAILED");
            throw e;
        }
        saveAndSendEnriched(event.getEventId(), "PRODUCT_UPDATED");
    }

    // --- Métodos de apoyo ---

    private boolean isInvalid(BaseEvent event) {
        if (event == null || event.getEventId() == null) {
            log.warn("⚠️ Recibido evento nulo o sin ID. Saltando...");
            return true;
        }
        return false;
    }

    private boolean isDuplicate(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

    private void saveAudit(UUID originalId, String type, String recipient, String status) {
        NotificationEntity audit = NotificationEntity.builder()
                .originalEventId(originalId)
                .eventType(type)
                .notificationType("LOG") // Valor por defecto
                .recipient(recipient)
                .status(status)
                .attemptCount(1)
                .dispatchedAt(Instant.now())
                .build();
        notificationRepository.save(audit);
    }

    private void saveAndSendEnriched(UUID eventId, String type) {
        processedEventRepository.save(new ProcessedEventEntity(eventId.toString(), Instant.now()));
        NotificationDispatchedEvent enriched = new NotificationDispatchedEvent();
        enriched.setEventId(eventId); 
        enriched.setOccurredAt(Instant.now());
        kafkaTemplate.send("domain.events.enriched", eventId.toString(), enriched);
    }
}