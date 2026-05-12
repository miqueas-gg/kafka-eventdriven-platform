package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.events.BaseEvent;
import com.kafkaeventdriven.events.OrderCreatedEvent;
import com.kafkaeventdriven.events.OrderStatusChangedEvent;
import com.kafkaeventdriven.events.ProductUpdatedEvent;
import com.kafkaeventdriven.notification.channels.NotificationChannel;
import com.kafkaeventdriven.notification.config.NotificationProperties;
import com.kafkaeventdriven.notification.entities.NotificationEntity;
import com.kafkaeventdriven.notification.repositories.NotificationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRepository repository;
    private final NotificationProperties properties;
    private final List<NotificationChannel> availableChannels;
    private final MeterRegistry meterRegistry;

    @Transactional(noRollbackFor = Exception.class)
    public void dispatchOrderCreated(OrderCreatedEvent event) {
        handleDispatch(event, "ORDER_CREATED", event.getCustomerEmail());
    }

    @Transactional
    public void dispatchOrderStatusChanged(OrderStatusChangedEvent event) {
        handleDispatch(event, "ORDER_STATUS_CHANGED", "customer@example.com");
    }

    @Transactional
    public void dispatchProductUpdated(ProductUpdatedEvent event) {
        handleDispatch(event, "PRODUCT_UPDATED", "admin@system.com");
    }

    private void handleDispatch(BaseEvent event, String eventType, String recipient) {
       
        NotificationEntity notification = repository.findByOriginalEventId(event.getEventId())
                .orElseGet(() -> {
                    NotificationEntity newEntity = new NotificationEntity();
                    newEntity.setOriginalEventId(event.getEventId());
                    newEntity.setEventType(eventType);
                    newEntity.setRecipient(recipient != null ? recipient : "sin-email@error.com");
                    newEntity.setAttemptCount(0);
                    newEntity.setStatus("PENDING");
                    newEntity.setNotificationType("MULTI_CHANNEL"); 
                    newEntity.setDispatchedAt(Instant.now());
                    return repository.saveAndFlush(newEntity);
                });

        // Si ya fue enviado con éxito, no hacemos nada (Idempotencia)
        if ("SENT".equals(notification.getStatus())) {
            log.info("Evento {} ya fue procesado con éxito anteriormente.", event.getEventId());
            return;
        }

        try {
            notification.setAttemptCount(notification.getAttemptCount() + 1);
            
            if (notification.getAttemptCount() > 1) {
                meterRegistry.counter("notification.retry.count").increment();
            }

            // Ejecutamos la lógica Strategy
            routeNotification(eventType, event, recipient);

            // Marcamos éxito en la persistencia única
            notification.setStatus("SENT");
            notification.setDispatchedAt(Instant.now());
            repository.saveAndFlush(notification);

        } catch (Exception e) {
           log.error("❌ Fallo real en el despacho para ID {}: {}", event.getEventId(), e.getMessage());
            
            // Actualizamos a FAILED y guardamos
            notification.setStatus("FAILED");
            repository.saveAndFlush(notification);
        }
    }

    private void routeNotification(String eventType, BaseEvent event, String recipient) {
        List<String> activeChannelNames = properties.getChannels().getOrDefault(eventType, List.of("LOG"));
        
        availableChannels.stream()
                .filter(channel -> activeChannelNames.contains(channel.getChannelType()))
                .forEach(channel -> {
                    Timer.Sample sample = Timer.start(meterRegistry);
                    String status = "SENT";
                    
                    try {
                        channel.dispatch(event, recipient);
                    } catch (Exception e) {
                        status = "FAILED";
                        log.error("Fallo en canal {}: {}", channel.getChannelType(), e.getMessage());
                        // No lanzamos excepción aquí para permitir que otros canales lo intenten
                        // o para que el handleDispatch capture el fallo final.
                        throw e; 
                    } finally {
                        sample.stop(meterRegistry.timer("notification.dispatch.time", 
                            "channel", channel.getChannelType()));
                        
                        meterRegistry.counter("notification.dispatched",
                                "event_type", eventType,
                                "channel", channel.getChannelType(),
                                "status", status).increment();
                    }
                });
    }
}