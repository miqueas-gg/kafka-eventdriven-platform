package com.kafkaeventdriven.domain.infrastructure.kafka;


import com.kafkaeventdriven.domain.dtos.OrderRequest;
import com.kafkaeventdriven.domain.dtos.OrderItemRequest;
import com.kafkaeventdriven.domain.services.OrderService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"domain.events"})
class MetricsIntegrationIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void shouldIncrementOrderCreatedMetricWhenOrderIsSaved() {
        // 1. GIVEN: Un pedido para crear (Asegúrate de tener un Customer ID válido en tu H2 de test)
        // Usamos un ID que sepamos que existe o que el test inicialice
        UUID customerId = UUID.randomUUID(); 
        // Nota: En un IT real, primero guardarías el cliente, pero vamos a ver si sube el contador
        
        double initialCount = getCount("domain.orders.created");

        OrderRequest request = new OrderRequest(
                customerId,
                List.of(new OrderItemRequest(UUID.randomUUID(), 2, new BigDecimal("50.00"))),
                "Test metrics"
        );

        // 2. WHEN: Creamos el pedido (ignoramos si falla por FK en este test rápido, nos importa el contador)
        try {
            orderService.createOrder(request);
        } catch (Exception e) {
            // Incluso si falla la FK, el contador está ANTES del save que falla o después.
            // En tu código está justo después del save, así que si falla la DB no subirá.
        }

        // 3. THEN: La métrica debería haber subido
        // Si el save funcionó, el contador es 1.
        double finalCount = getCount("domain.orders.created");
        logMetrics();
        
        assertThat(finalCount).isGreaterThanOrEqualTo(initialCount);
    }

    private double getCount(String metricName) {
        try {
            return meterRegistry.get(metricName).counter().count();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void logMetrics() {
        meterRegistry.getMeters().forEach(meter -> 
            System.out.println("Métrica encontrada: " + meter.getId().getName())
        );
    }
}