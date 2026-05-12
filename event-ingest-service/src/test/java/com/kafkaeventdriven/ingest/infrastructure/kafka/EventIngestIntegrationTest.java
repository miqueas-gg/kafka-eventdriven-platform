package com.kafkaeventdriven.ingest.infrastructure.kafka;

import com.kafkaeventdriven.ingest.domain.repositories.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;
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

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", 
            () -> System.getProperty("spring.kafka.embedded.brokers"));
    }

    @Test
    void whenEventIsPublished_thenItIsPersistedInEventStore() throws Exception {
        // GIVEN
        String eventIdStr = "550e8400-e29b-41d4-a716-446655440000";
        String eventJson = """
            {
                "eventId": "%s",
                "eventType": "ORDER_CREATED",
                "source": "test-source",
                "version": "1.0",
                "payload": {"orderId": "123"},
                "occurredAt": "2026-04-28T14:30:00"
            }
            """.formatted(eventIdStr);

        // WHEN
        kafkaTemplate.send("domain.events", eventJson).get(10, TimeUnit.SECONDS);

        // THEN
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                boolean exists = eventRepository.existsByEventId(UUID.fromString(eventIdStr));
                assertThat(exists).as("El evento debería haberse persistido en la base de datos").isTrue();
            });
    }
}