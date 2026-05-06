package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.events.*;
import com.kafkaeventdriven.notification.entities.NotificationEntity;
import com.kafkaeventdriven.notification.entities.ProcessedEventEntity;
import com.kafkaeventdriven.notification.repositories.NotificationRepository; // El nuevo repo
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
@RequiredArgsConstructor // Esto inyecta automáticamente los "final"
@KafkaListener(topics = "domain.events")
public class NotificationConsumer {

    private final NotificationDispatcher dispatcher;
    private final ProcessedEventRepository processedEventRepository;
    private final NotificationRepository notificationRepository; // Inyectado
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaHandler
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (isDuplicate(event.getEventId().toString())) return;
        
        try {
            dispatcher.dispatchOrderCreated(event);
            saveAudit(event.getEventId(), "ORDER_CREATED", event.getCustomerEmail(), "SENT");
        } catch (Exception e) {
            saveAudit(event.getEventId(), "ORDER_CREATED", event.getCustomerEmail(), "FAILED");
            throw e; // Para que actúe el RetryableTopic
        }
        
        saveAndSendEnriched(event.getEventId(), "ORDER_CREATED");
    }

    // Repite la misma lógica de try-catch para los otros @KafkaHandler...

    private void saveAudit(UUID originalId, String type, String recipient, String status) {
        NotificationEntity audit = NotificationEntity.builder()
                .originalEventId(originalId)
                .eventType(type)
                .notificationType("LOG")
                .recipient(recipient)
                .status(status)
                .attemptCount(1) // En issues futuras lo haremos dinámico
                .dispatchedAt(Instant.now())
                .build();
        
        notificationRepository.save(audit);
    }

    private boolean isDuplicate(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

    private void saveAndSendEnriched(UUID eventId, String type) {
        processedEventRepository.save(new ProcessedEventEntity(eventId.toString(), Instant.now()));
        NotificationDispatchedEvent enriched = new NotificationDispatchedEvent();
        enriched.setEventId(eventId); 
        enriched.setOccurredAt(Instant.now());
        kafkaTemplate.send("domain.events.enriched", eventId.toString(), enriched);
    }
}