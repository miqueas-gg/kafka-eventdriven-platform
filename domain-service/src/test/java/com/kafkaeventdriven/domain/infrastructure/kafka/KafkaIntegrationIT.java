package com.kafkaeventdriven.domain.infrastructure.kafka;

import com.kafkaeventdriven.domain.dtos.OrderItemRequest;
import com.kafkaeventdriven.domain.dtos.OrderRequest;
import com.kafkaeventdriven.domain.services.OrderService;
import com.kafkaeventdriven.domain.repositories.OrderRepository; // Añadido
import com.kafkaeventdriven.events.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach; // Añadido
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.kafka.topic-name=domain.events",
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"domain.events"})
@ActiveProfiles("test")
@Sql(scripts = "/sql/data-test.sql")
class KafkaIntegrationIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository; // Para debug

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

    @KafkaListener(topics = "domain.events", groupId = "test-group")
    public void listen(String message) {
        System.out.println("!!! MENSAJE RECIBIDO EN TEST: " + message);
        messages.add(message);
    }

    @BeforeEach
    void setUp() {
        messages.clear();
        orderRepository.deleteAll(); // Limpieza vital
    }

    @Test
    void shouldPublishOrderCreatedEventWhenOrderIsCreated() throws Exception {
        // 1. IMPORTANTE: Esperamos un poco a que el consumidor de Kafka esté "vivo"
        Thread.sleep(2000); 

        // GIVEN
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("770e8400-e29b-41d4-a716-446655441111");
        OrderRequest request = new OrderRequest(customerId, 
            List.of(new OrderItemRequest(productId, 2, new BigDecimal("25.50"))), "Test");

        // WHEN
        try {
            orderService.createOrder(request);
            kafkaTemplate.flush();
        } catch (Exception e) {
            System.err.println("EL SERVICE HA PETADO: " + e.getMessage());
            throw e;
        }

        // DEBUG: Ver si el pedido se guardó
        System.out.println("PEDIDOS EN BD: " + orderRepository.count());

        // THEN
        String receivedMessage = messages.poll(20, TimeUnit.SECONDS);
        
        assertThat(receivedMessage).isNotNull();
        OrderCreatedEvent event = objectMapper.readValue(receivedMessage, OrderCreatedEvent.class);
        assertThat(event.getCustomerId()).isEqualTo(customerId);
    }
}