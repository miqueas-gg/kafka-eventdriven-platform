package com.kafkaeventdriven.domain.infrastructure.kafka;

import com.kafkaeventdriven.events.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;


import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaEventPublisher kafkaEventPublisher;

    @Test
    void shouldCallKafkaTemplateWhenPublishing() {
        // GIVEN
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(UUID.randomUUID())
                .aggregateId(UUID.randomUUID().toString())
                .customerId(UUID.randomUUID())
                .totalAmount(BigDecimal.TEN)
                .eventId(UUID.randomUUID())
                .occurredAt(java.time.Instant.now())
                .build();
        
        // Simulamos la respuesta asíncrona de Kafka
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

        // WHEN
        kafkaEventPublisher.publish(event, "test-topic");

        // THEN
        verify(kafkaTemplate, times(1)).send(eq("test-topic"), anyString(), eq(event));
    }
}