package com.kafkaeventdriven.domain.services;

import com.kafkaeventdriven.domain.dtos.*;
import com.kafkaeventdriven.domain.entities.OutboxEvent;
import com.kafkaeventdriven.domain.repositories.OrderRepository;
import com.kafkaeventdriven.domain.repositories.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092", // Pon una IP falsa o la de tu docker
    "spring.kafka.consumer.auto-start=false",        // Evita que los listeners intenten arrancar
    "spring.kafka.listener.type=batch"               // Configuración mínima
})
@ActiveProfiles("test")
public class OrderServiceOutboxTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    // Usamos @MockBean solo cuando queramos forzar el fallo
    @MockBean
    private OutboxRepository outboxRepository;

    @Test
    void atomicRollbackTest() {
        // GIVEN: Forzamos que el repositorio de Outbox lance una excepción
        when(outboxRepository.save(any(OutboxEvent.class)))
            .thenThrow(new RuntimeException("Error simulado en la tabla Outbox"));

        OrderRequest request = createMockOrderRequest();

        // WHEN & THEN: Verificamos que se lanza la excepción
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(request);
        });

        // VERIFICACIÓN FINAL: El pedido NO debe existir en la DB (Atomicidad)
        // Buscamos si hay alguna orden en la tabla. Debería estar vacía.
        assertTrue(orderRepository.findAll().isEmpty(), 
            "El pedido se guardó en la DB pero debería haberse borrado por el Rollback!");
    }

    // --- ESTE ES EL MÉTODO QUE TE FALTABA ---
    private OrderRequest createMockOrderRequest() {
        // Ajusta los nombres de los campos según tu OrderRequest DTO
        return new OrderRequest(
            UUID.randomUUID(), // customerId
            List.of(new OrderItemRequest(UUID.randomUUID(), 2, new BigDecimal("10.00"))), // items
            "Test de atomicidad Outbox" // notes
        );
    }
}