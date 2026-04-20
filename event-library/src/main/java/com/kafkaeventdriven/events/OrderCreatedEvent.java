package com.kafkaeventdriven.events;

import lombok.Getter;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@SuperBuilder 
public class OrderCreatedEvent  extends BaseEvent{
    private UUID orderId;
    private UUID customerId;
    private String customerEmail;
    private List<OrderItemDto> items;
    private BigDecimal totalAmount;
    private String status;

    {
        setEventType("ORDER_CREATED");
    }
}
