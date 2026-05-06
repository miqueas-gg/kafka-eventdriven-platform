package com.kafkaeventdriven.notification;
import com.kafkaeventdriven.events.OrderCreatedEvent;
import com.kafkaeventdriven.events.OrderStatusChangedEvent;
import com.kafkaeventdriven.events.ProductUpdatedEvent;
import com.kafkaeventdriven.notification.channels.NotificationChannel;
import com.kafkaeventdriven.notification.config.NotificationProperties;
import com.kafkaeventdriven.notification.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class NotificationDispatcherTest {

    private NotificationRepository repository;
    private NotificationProperties properties;
    private List<NotificationChannel> channels;
    private NotificationDispatcher dispatcher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // 1. Mocks de los repositorios y propiedades
        repository = Mockito.mock(NotificationRepository.class);
        properties = Mockito.mock(NotificationProperties.class);
        
        // 2. Mock de un canal (por ejemplo el de LOG) para que la lista no esté vacía
        NotificationChannel logChannel = Mockito.mock(NotificationChannel.class);
        when(logChannel.getChannelType()).thenReturn("LOG");
        channels = List.of(logChannel);

        // 3. Configuramos las propiedades para que devuelvan un mapa válido
        // Esto evita el NullPointerException cuando el dispatcher llame a properties.getChannels()
        when(properties.getChannels()).thenReturn(Map.of(
            "ORDER_CREATED", List.of("LOG"),
            "ORDER_STATUS_CHANGED", List.of("LOG"),
            "PRODUCT_UPDATED", List.of("LOG")
        ));

        // 4. Pasamos los TRES argumentos al nuevo constructor
        dispatcher = new NotificationDispatcher(repository, properties, channels);

        // 5. Comportamiento por defecto del repo
        when(repository.findByOriginalEventId(any())).thenReturn(Optional.empty());
        // Importante: mockear saveAndFlush para evitar NPE
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void shouldHandleOrderCreatedLog() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
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