package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.events.OrderCreatedEvent;
import com.kafkaeventdriven.events.OrderStatusChangedEvent;
import com.kafkaeventdriven.events.ProductUpdatedEvent;
import com.kafkaeventdriven.notification.repositories.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class NotificationDispatcherTest {

   private NotificationRepository repository;
    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        // 1. Creamos un mock del repositorio
        repository = Mockito.mock(NotificationRepository.class);
        
        // 2. Se lo pasamos al constructor que generó @RequiredArgsConstructor
        dispatcher = new NotificationDispatcher(repository);

        // 3. Configuramos un comportamiento por defecto para que no falle al buscar
        when(repository.findByOriginalEventId(any())).thenReturn(Optional.empty());
    }

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