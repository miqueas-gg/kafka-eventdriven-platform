package com.kafkaeventdriven.events;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.UUID;

@Getter
@NoArgsConstructor
@SuperBuilder
public class ProductUpdatedEvent extends BaseEvent {
    private UUID productId;
    private String productName;
    private String changedField;
    private String previousValue;
    private String newValue;

    {
        setEventType("PRODUCT_UPDATED");
    }
}