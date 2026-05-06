package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.events.OrderCreatedEvent;
import com.kafkaeventdriven.notification.entities.NotificationEntity;
import com.kafkaeventdriven.notification.repositories.NotificationRepository;
import lombok.extern.slf4j.Slf4j; // Asegúrate de tener esta importación
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;


@Slf4j // 1. Soluciona el error de 'log'
@SpringBootTest(properties = {
    // 1. Forzamos que el productor mande JSON de verdad
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
    // 2. IMPORTANTE: Forzamos al consumidor a convertir ese JSON al objeto correcto
    "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
    "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer",
    "spring.kafka.consumer.properties.spring.json.value.default.type=com.kafkaeventdriven.events.OrderCreatedEvent",
    "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
    "spring.kafka.consumer.group-id=notification-test-unique-v3",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DirtiesContext
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"domain.events"}) // 2. Quitamos 'properties' de aquí para evitar el error
public class NotificationRetryIntegrationTest {

    @Autowired
    private NotificationRepository repository;

    @org.junit.jupiter.api.BeforeEach
   void cleanUp() {
    log.info("Limpiando base de datos antes del test...");
    repository.deleteAll();
    repository.flush(); // Asegura que el borrado es total
}
    @Autowired // 3. Soluciona el error de 'kafkaTemplate'
    private KafkaTemplate<String, Object> kafkaTemplate;

    @SpyBean
    private NotificationDispatcher dispatcher;

    @Test
    void testRetryPolicyAndAttemptCount() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(UUID.randomUUID())
                .customerEmail("test@example.com")
                .build();

        // Forzamos el error para que Kafka reintente
        doAnswer(invocation -> {
            OrderCreatedEvent arg = invocation.getArgument(0);
            log.info("Spy interceptó el evento: {}", arg.getEventId());
            throw new RuntimeException("Error simulado para reintentos");
        }).when(dispatcher).dispatchOrderCreated(any(OrderCreatedEvent.class));
        // Enviamos el mensaje
        kafkaTemplate.send("domain.events", event);

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            List<NotificationEntity> entities = repository.findAllByOriginalEventId(eventId);
            
            log.info("Registros con el mismo ID en DB: {}", entities.size());
            
            // Si hay más de 1 registro con el mismo originalEventId, 
            // es PRUEBA de que Kafka ha reintentado (porque el rollback generó duplicados)
            assertTrue(entities.size() >= 2, "Kafka no ha reintentado (solo hay 1 registro)");
            
            // Verificamos que al menos uno esté en FAILED
            boolean hasFailed = entities.stream().anyMatch(e -> "FAILED".equals(e.getStatus()));
            assertTrue(hasFailed, "Ningún registro quedó en estado FAILED");
        });
    }
}