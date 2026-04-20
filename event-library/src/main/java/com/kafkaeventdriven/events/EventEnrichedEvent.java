package com.kafkaeventdriven.events;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class EventEnrichedEvent extends BaseEvent {
    private UUID originalEventId;
    private Map<String, Object> enrichedFields; // Flexibilidad total para añadir datos
    private String enrichmentSource;

    {
        setEventType("EVENT_ENRICHED");
    }
}