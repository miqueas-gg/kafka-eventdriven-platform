package com.kafkaeventdriven.events;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder 
public class OrderCreatedEvent  extends BaseEvent{
    private UUID orderId;
    private UUID customerId;
    private String customerEmail;
    private List<OrderItemDto> items;
    private BigDecimal totalAmount;
    private String status;

    // CONSTRUCTOR MANUAL: Es la forma más fiable de inicializar todo
    public OrderCreatedEvent(String orderId, UUID customerId, BigDecimal totalAmount) {
        this.setAggregateId(orderId); 
        this.orderId = UUID.fromString(orderId);     // El ID del pedido para la Key de Kafka
        this.setEventId(UUID.randomUUID()); // Identificador único del mensaje
        this.setOccurredAt(Instant.now());
        this.setEventType("ORDER_CREATED");
        this.customerId = customerId;
        this.totalAmount = totalAmount;
    }

}
