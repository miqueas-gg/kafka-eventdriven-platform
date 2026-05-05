package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.events.OrderCreatedEvent;
import com.kafkaeventdriven.events.OrderStatusChangedEvent;
import com.kafkaeventdriven.events.ProductUpdatedEvent;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationDispatcherTest {

    private final NotificationDispatcher dispatcher = new NotificationDispatcher();

    @Test
    void shouldHandleOrderCreatedLog() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(UUID.randomUUID()) // Cambiado de String a UUID
                .customerEmail("cliente@correo.com")
                .build();

        assertDoesNotThrow(() -> dispatcher.dispatchOrderCreated(event));
    }

    @Test
    void shouldHandleOrderStatusChangedLog() {
        OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(UUID.randomUUID()) // Cambiado de String a UUID
                .newStatus("SHIPPED")
                .build();

        assertDoesNotThrow(() -> dispatcher.dispatchOrderStatusChanged(event));
    }

    @Test
    void shouldHandleProductPriceUpdatedLog() {
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .productId(UUID.randomUUID()) // Asegúrate de que productId también sea UUID
                .productName("Teclado Mecánico")
                .changedField("price")
                .previousValue("50.0")
                .newValue("45.0")
                .build();

        assertDoesNotThrow(() -> dispatcher.dispatchProductUpdated(event));
    }
}