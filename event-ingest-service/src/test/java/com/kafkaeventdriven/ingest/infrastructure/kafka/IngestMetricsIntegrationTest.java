package com.kafkaeventdriven.ingest.infrastructure.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"domain.events", "domain.events.dlt"})
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:metricstestdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS events",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "management.server.port=8082",
    // 2. FORZAMOS el tópico para que el test y el código usen el mismo
    "app.kafka.topics.ingest=domain.events", 
    // 3. Reducimos el tiempo de espera del consumidor para que el test sea más rápido
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DirtiesContext
class IngestMetricsIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MeterRegistry meterRegistry; // El espía de métricas

    @Test
    void shouldIncrementMetricsWhenEventIsProcessed() {
        // GIVEN: Un evento válido
        String validEvent = "{" +
                "\"event_id\":\"" + java.util.UUID.randomUUID() + "\"," +
                "\"event_type\":\"ORDER_CREATED\"," +
                "\"source\":\"test-source\"," +
                "\"payload\":{}," +
                "\"occurred_at\":\"2026-04-29T13:00:00Z\"" +
                "}";

        // WHEN: Enviamos el evento a Kafka
        kafkaTemplate.send("domain.events", validEvent);

        // THEN: Verificamos que los contadores suben
        // Usamos Awaitility porque el consumo de Kafka es asíncrono
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // Buscamos el contador por nombre
            var receivedCounter = meterRegistry.find("ingest.events.received").counter();
            assertThat(receivedCounter).isNotNull();
            assertThat(receivedCounter.count()).isGreaterThanOrEqualTo(1.0);
        });
    }

    @Test
    void shouldIncrementInvalidMetricWhenEventIsMalformed() {
        // GIVEN: Un evento basura (sin UUID)
        String invalidEvent = "{\"invalid\":\"data\"}";

        // WHEN: Enviamos a Kafka
        kafkaTemplate.send("domain.events", invalidEvent);

        // THEN: El contador de invalid debe subir
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            double invalidCount = meterRegistry.counter("ingest.events.invalid").count();
            assertThat(invalidCount).isGreaterThanOrEqualTo(1.0);
        });
    }
}