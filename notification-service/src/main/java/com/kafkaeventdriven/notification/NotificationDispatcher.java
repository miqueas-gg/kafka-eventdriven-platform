package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.events.BaseEvent;
import com.kafkaeventdriven.events.OrderCreatedEvent;
import com.kafkaeventdriven.events.OrderStatusChangedEvent;
import com.kafkaeventdriven.events.ProductUpdatedEvent;
import com.kafkaeventdriven.notification.channels.NotificationChannel;
import com.kafkaeventdriven.notification.config.NotificationProperties;
import com.kafkaeventdriven.notification.entities.NotificationEntity;
import com.kafkaeventdriven.notification.repositories.NotificationRepository;
import io.micrometer.core.instrument.MeterRegistry; // Importante
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRepository repository;
    private final NotificationProperties properties;
    private final List<NotificationChannel> availableChannels;
    private final MeterRegistry meterRegistry;

    @Transactional
    public void dispatchOrderCreated(OrderCreatedEvent event) {
        handleDispatch(event, "ORDER_CREATED", event.getCustomerEmail());
    }

    @Transactional
    public void dispatchOrderStatusChanged(OrderStatusChangedEvent event) {
        // Para este evento, podrías necesitar el email del cliente, 
        // aquí usamos un placeholder o lo extraemos si tu evento lo tiene
        handleDispatch(event, "ORDER_STATUS_CHANGED", "customer@example.com");
    }

    @Transactional
    public void dispatchProductUpdated(ProductUpdatedEvent event) {
        // Los cambios de producto suelen ser para admins o logs internos
        handleDispatch(event, "PRODUCT_UPDATED", "admin@system.com");
    }

    private void handleDispatch(BaseEvent event, String eventType, String recipient) {
        NotificationEntity notification = repository.findByOriginalEventId(event.getEventId())
                .orElseGet(() -> {
                    NotificationEntity newEntity = new NotificationEntity();
                    newEntity.setOriginalEventId(event.getEventId());
                    newEntity.setEventType(eventType);
                    newEntity.setAttemptCount(0);
                    newEntity.setStatus("PENDING");
                    return repository.saveAndFlush(newEntity);
                });

        try {
            notification.setAttemptCount(notification.getAttemptCount() + 1);
            
            if (notification.getAttemptCount() > 1) {
                meterRegistry.counter("notification.retry.count").increment();
            }
            // Lógica Strategy
            routeNotification(eventType, event, recipient);

            notification.setStatus("SENT");
            notification.setDispatchedAt(Instant.now());
            repository.saveAndFlush(notification);

        } catch (Exception e) {
            notification.setStatus("FAILED");
            repository.saveAndFlush(notification);
            throw e;
        }
    }

    private void routeNotification(String eventType, BaseEvent event, String recipient) {
        List<String> activeChannelNames = properties.getChannels().getOrDefault(eventType, List.of("LOG"));
        
        availableChannels.stream()
                .filter(channel -> activeChannelNames.contains(channel.getChannelType()))
                .forEach(channel -> {
                    // Métrica: notification.dispatch.time (Timer)
                    Timer.Sample sample = Timer.start(meterRegistry);
                    String status = "SENT";
                    
                    try {
                        channel.dispatch(event, recipient);
                    } catch (Exception e) {
                        status = "FAILED";
                        throw e;
                    } finally {
                        sample.stop(meterRegistry.timer("notification.dispatch.time", 
                            "channel", channel.getChannelType()));
                        
                        // Métrica: notification.dispatched (Counter con tags)
                        meterRegistry.counter("notification.dispatched",
                                "event_type", eventType,
                                "channel", channel.getChannelType(),
                                "status", status).increment();
                    }
                });
    }
}