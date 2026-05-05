package com.kafkaeventdriven.events;

import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // Necesario para que el Builder funcione bien con todos los campos
@SuperBuilder 
public class OrderCreatedEvent extends BaseEvent {
    
    // ESTA es la forma correcta de usar Builder.Default
    @Builder.Default
    private String eventType = "ORDER_CREATED";

    private UUID orderId;
    private UUID customerId;
    private String customerEmail;
    private List<OrderItemDto> items;
    private BigDecimal totalAmount;
    private String status;
    private String source;
}