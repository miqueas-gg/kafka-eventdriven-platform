package com.kafkaeventdriven.domain.infrastructure.kafka;

import com.kafkaeventdriven.domain.dtos.OrderItemRequest;
import com.kafkaeventdriven.domain.dtos.OrderRequest;
import com.kafkaeventdriven.domain.repositories.OrderRepository;
import com.kafkaeventdriven.domain.services.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = {
    "app.kafka.topic-name=failure.events", // Tópico distinto para evitar ruido
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
@DirtiesContext
@EmbeddedKafka(partitions = 1)
@ActiveProfiles("test")
class KafkaFailureRecoveryIT {

    @SpyBean
    private KafkaEventPublisher eventPublisher;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanDatabase() {
        // Esto borra todos los pedidos antes de empezar el test
        orderRepository.deleteAll();
    }

    @Test
    void whenKafkaFails_thenTransactionShouldRollback() {
        // GIVEN
        OrderRequest request = new OrderRequest(UUID.randomUUID(), 
            List.of(new OrderItemRequest(UUID.randomUUID(), 1, new BigDecimal("10.0"))), "Failure Test");

        // Forzamos el error
        doThrow(new RuntimeException("Kafka Down")).when(eventPublisher).publish(any(), any());

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> orderService.createOrder(request));

        // VERIFICACIÓN: El pedido NO debe estar en la BD
        assertThat(orderRepository.count()).isZero();
    }
}