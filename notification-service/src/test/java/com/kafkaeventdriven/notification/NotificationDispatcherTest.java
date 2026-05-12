package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.events.OrderCreatedEvent;
import com.kafkaeventdriven.events.OrderStatusChangedEvent;
import com.kafkaeventdriven.events.ProductUpdatedEvent;
import com.kafkaeventdriven.notification.channels.NotificationChannel;
import com.kafkaeventdriven.notification.config.NotificationProperties;
import com.kafkaeventdriven.notification.repositories.NotificationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
    private MeterRegistry meterRegistry; // Nuevo campo necesario
    private NotificationDispatcher dispatcher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // 1. Mocks de los repositorios y propiedades
        repository = Mockito.mock(NotificationRepository.class);
        properties = Mockito.mock(NotificationProperties.class);
        
        // 2. Usamos SimpleMeterRegistry (una implementación real de memoria para tests)
        // Esto es mejor que un mock porque evita configurar comportamientos del meterRegistry
        meterRegistry = new SimpleMeterRegistry();
        
        // 3. Mock de un canal (LOG)
        NotificationChannel logChannel = Mockito.mock(NotificationChannel.class);
        when(logChannel.getChannelType()).thenReturn("LOG");
        channels = List.of(logChannel);

        // 4. Configuración de propiedades
        when(properties.getChannels()).thenReturn(Map.of(
            "ORDER_CREATED", List.of("LOG"),
            "ORDER_STATUS_CHANGED", List.of("LOG"),
            "PRODUCT_UPDATED", List.of("LOG")
        ));

        // 5. ¡AQUÍ ESTÁ LA CORRECCIÓN! Pasamos los CUATRO argumentos
        dispatcher = new NotificationDispatcher(repository, properties, channels, meterRegistry);

        // 6. Comportamiento por defecto del repo
        when(repository.findByOriginalEventId(any())).thenReturn(Optional.empty());
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
                .orderId(UUID.randomUUID())
                .newStatus("SHIPPED")
                .build();

        assertDoesNotThrow(() -> dispatcher.dispatchOrderStatusChanged(event));
    }

    @Test
    void shouldHandleProductPriceUpdatedLog() {
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .productName("Teclado Mecánico")
                .build();

        assertDoesNotThrow(() -> dispatcher.dispatchProductUpdated(event));
    }
}