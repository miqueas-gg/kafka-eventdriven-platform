package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.events.*;
import com.kafkaeventdriven.notification.entities.NotificationEntity;
import com.kafkaeventdriven.notification.repositories.NotificationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRepository repository;

// En NotificationDispatcher.java

@Transactional
public void dispatchOrderCreated(OrderCreatedEvent event) {
    // 1. Aseguramos que la entidad exista (esto no debe fallar)
    NotificationEntity notification = repository.findByOriginalEventId(event.getEventId())
            .orElseGet(() -> {
                NotificationEntity newEntity = new NotificationEntity();
                newEntity.setOriginalEventId(event.getEventId());
                newEntity.setEventType("ORDER_CREATED");
                newEntity.setAttemptCount(0);
                newEntity.setStatus("PENDING");
                return repository.saveAndFlush(newEntity); 
            });

    try {
        // 2. Incrementamos el contador manualmente
        notification.setAttemptCount(notification.getAttemptCount() + 1);
        log.info(">>> EJECUTANDO INTENTO {} PARA EVENTO {}", 
                 notification.getAttemptCount(), event.getEventId());
        
        // 3. Lógica que será interceptada por el Spy y lanzará error
        // ...
        
        notification.setStatus("SENT");
        repository.saveAndFlush(notification);

    } catch (Exception e) {
        // 4. Si falla, marcamos FAILED y guardamos
        notification.setStatus("FAILED");
        repository.saveAndFlush(notification);
        throw e; 
    }
}
    public void dispatchOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Notificando al cliente que su pedido {} pasó a estado {}", 
            event.getOrderId(), event.getNewStatus());
    }

    public void dispatchProductUpdated(ProductUpdatedEvent event) {
        // Según tu clase: el criterio es que el campo cambiado sea el precio
        if ("price".equalsIgnoreCase(event.getChangedField())) {
            log.info("Alerta: el precio del producto {} cambió de {} a {}", 
                event.getProductName(), 
                event.getPreviousValue(), 
                event.getNewValue());
        }
    }
}