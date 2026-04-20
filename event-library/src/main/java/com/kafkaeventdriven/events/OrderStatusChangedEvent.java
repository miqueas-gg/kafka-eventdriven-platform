package com.kafkaeventdriven.events;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.UUID;

@Getter
@NoArgsConstructor
@SuperBuilder
public class OrderStatusChangedEvent extends BaseEvent {
    private UUID orderId;
    private String previousStatus;
    private String newStatus;
    private String reason;

    {
        setEventType("ORDER_STATUS_CHANGED");
    }
}