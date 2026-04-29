package com.kafkaeventdriven.domain.infrastructure.kafka;

import com.kafkaeventdriven.domain.entities.DeadLetterEvent;
import com.kafkaeventdriven.domain.repositories.DeadLetterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test") // Usa un application-test.properties si tienes uno
@EmbeddedKafka(partitions = 1, topics = {"domain.events.DLT"})
public class DltConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private DeadLetterRepository deadLetterRepository;

    @Test
    void whenMessageIsSentToDlt_thenItIsPersistedInDatabase() {
        // 1. GIVEN: Un mensaje JSON que simula un evento fallido
        String eventPayload = "{\"orderId\":\"b51bb01e\", \"status\":\"PAID\"}";
        deadLetterRepository.deleteAll(); // Limpiamos la morgue

        // 2. WHEN: Enviamos el mensaje directamente al topic DLT
        kafkaTemplate.send("domain.events.DLT", eventPayload);

        // 3. THEN: Esperamos a que el consumidor lo guarde (los tests de Kafka son asíncronos)
        await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                List<DeadLetterEvent> events = deadLetterRepository.findAll();
                assertThat(events).hasSize(1);
                assertThat(events).isNotEmpty(); // Primero vemos si hay algo
        
                // Usamos una comparación que ignore los escapes de las comillas
                String payloadGuardado = events.get(0).getPayload();
                assertThat(payloadGuardado).contains("orderId");
                assertThat(payloadGuardado).contains("b51bb01e");
            });
            
        System.out.println("✅ Test superado: El evento ha resucitado en la DB de tests");
    }
}