package com.kafkaeventdriven.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EventSerializationTest {
  
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldSerializeAndDeserializeOrderCreatedEvent() throws Exception {
        // 1. Creamos un evento de prueba
        OrderCreatedEvent originalEvent = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(Instant.now())
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerEmail("test@example.com")
                .totalAmount(new BigDecimal("150.50"))
                .items(List.of(
                    OrderItemDto.builder()
                        .productId(UUID.randomUUID())
                        .productName("Teclado Mecánico")
                        .quantity(1)
                        .unitPrice(new BigDecimal("150.50"))
                        .build()
                ))
                .status("PENDING")
                .source("domain-service")
                .build();

        String json = mapper.writeValueAsString(originalEvent);
        System.out.println("JSON Result: " + json);
        
        OrderCreatedEvent deserialized = mapper.readValue(json, OrderCreatedEvent.class);

        assertEquals("ORDER_CREATED", deserialized.getEventType());
        assertEquals(originalEvent.getOrderId(), deserialized.getOrderId());
        assertEquals(originalEvent.getTotalAmount(), deserialized.getTotalAmount());
        assertEquals(1, deserialized.getItems().size());
        assertEquals("Teclado Mecánico", deserialized.getItems().get(0).getProductName());
    }

    @Test
void shouldSerializeAndDeserializeNotificationEvent() throws Exception {
    NotificationDispatchedEvent event = NotificationDispatchedEvent.builder()
            .eventId(UUID.randomUUID())
            .originalEventId(UUID.randomUUID())
            .notificationType("EMAIL")
            .recipient("user@example.com")
            .status("SENT")
            .attemptCount(1)
            .build();

    String json = mapper.writeValueAsString(event);
    NotificationDispatchedEvent deserialized = mapper.readValue(json, NotificationDispatchedEvent.class);

    assertEquals("NOTIFICATION_DISPATCHED", deserialized.getEventType());
    assertEquals(1, deserialized.getAttemptCount());
}

@Test
void shouldSerializeAndDeserializeEnrichedEvent() throws Exception {
    Map<String, Object> extraData = Map.of("customerRiskScore", 85, "isVip", true);
    
    EventEnrichedEvent event = EventEnrichedEvent.builder()
            .eventId(UUID.randomUUID())
                .enrichedFields(extraData)
            .enrichmentSource("risk-analysis-service")
            .build();

    String json = mapper.writeValueAsString(event);
    EventEnrichedEvent deserialized = mapper.readValue(json, EventEnrichedEvent.class);

    assertEquals("risk-analysis-service", deserialized.getEnrichmentSource());
    assertEquals(85, deserialized.getEnrichedFields().get("customerRiskScore"));
}
}
