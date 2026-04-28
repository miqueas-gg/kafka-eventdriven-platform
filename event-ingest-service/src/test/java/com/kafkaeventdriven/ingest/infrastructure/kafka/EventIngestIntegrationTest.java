package com.kafkaeventdriven.ingest.infrastructure.kafka;

import com.kafkaeventdriven.ingest.domain.repositories.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"domain.events"})
class EventIngestIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void whenEventIsPublished_thenItIsPersistedInEventStore() {
        // 1. Preparamos un evento de prueba (JSON bruto)
        String eventJson = """
            {
                "eventId": "550e8400-e29b-41d4-a716-446655440000",
                "eventType": "ORDER_CREATED",
                "source": "domain-service",
                "payload": {"orderId": "123"},
                "occurredAt": "2026-04-28T14:30:00"
            }
            """;

        // 2. Lo enviamos al topic que escucha nuestro Ingest
        kafkaTemplate.send("domain.events", eventJson);

        // 3. Verificamos que el servicio lo ha guardado en la base de datos
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            boolean exists = eventRepository.existsByEventId(
                java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
            );
            assertThat(exists).isTrue();
        });
    }
}