package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.events.OrderCreatedEvent;
import com.kafkaeventdriven.notification.channels.NotificationChannel;
import com.kafkaeventdriven.notification.config.NotificationProperties;
import com.kafkaeventdriven.notification.entities.NotificationEntity;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class NotificationMetricsTest {

    private NotificationRepository repository;
    private NotificationProperties properties;
    private MeterRegistry meterRegistry; // Usaremos la implementación real de memoria
    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(NotificationRepository.class);
        properties = Mockito.mock(NotificationProperties.class);
        meterRegistry = new SimpleMeterRegistry(); // <-- Registro limpio para cada test

        // Mock del canal de LOG
        NotificationChannel logChannel = Mockito.mock(NotificationChannel.class);
        when(logChannel.getChannelType()).thenReturn("LOG");
        List<NotificationChannel> channels = List.of(logChannel);

        // Configuración de propiedades
        when(properties.getChannels()).thenReturn(Map.of("ORDER_CREATED", List.of("LOG")));

        // Instanciamos el Dispatcher con el meterRegistry real (Simple)
        dispatcher = new NotificationDispatcher(repository, properties, channels, meterRegistry);

        // Mocks de repositorio para evitar NPE
        when(repository.findByOriginalEventId(any())).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void shouldIncrementMetricsWhenDispatching() {
        // Arrange
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .customerEmail("test@test.com")
                .build();

        // Act
        dispatcher.dispatchOrderCreated(event);

        // Assert: Verificar contador de despacho
        double count = meterRegistry.get("notification.dispatched")
                .tag("event_type", "ORDER_CREATED")
                .tag("channel", "LOG")
                .tag("status", "SENT")
                .counter().count();

        assertEquals(1.0, count, "El contador notification.dispatched debería ser 1");
        
        // Assert: Verificar que el Timer se ha registrado
        double timerCount = meterRegistry.get("notification.dispatch.time")
                .tag("channel", "LOG")
                .timer().count();
        
        assertEquals(1.0, timerCount, "El timer debería haber registrado 1 ejecución");
    }

    @Test
    void shouldIncrementRetryMetricOnSecondAttempt() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .build();

        // Simulamos que ya existía una entidad con 1 intento previo
        NotificationEntity existingEntity = new NotificationEntity();
        existingEntity.setAttemptCount(1);
        when(repository.findByOriginalEventId(eventId)).thenReturn(Optional.of(existingEntity));

        // Act
        dispatcher.dispatchOrderCreated(event);

        // Assert: El contador de reintentos debe haber subido
        double retryCount = meterRegistry.get("notification.retry.count").counter().count();
        assertEquals(1.0, retryCount, "La métrica de reintentos debería haber incrementado");
    }
}