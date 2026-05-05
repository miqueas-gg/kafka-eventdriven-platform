package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.events.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationDispatcher {

    public void dispatchOrderCreated(OrderCreatedEvent event) {
        log.info("Notificando al cliente {} que su pedido {} fue creado", 
            event.getCustomerEmail(), event.getOrderId());
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