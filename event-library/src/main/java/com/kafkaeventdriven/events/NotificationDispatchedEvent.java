package com.kafkaeventdriven.events;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class NotificationDispatchedEvent extends BaseEvent {
    private UUID originalEventId;
    private String notificationType; // EMAIL, LOG, WEBHOOK
    private String recipient;
    private String status; // SENT, FAILED
    private int attemptCount;

    {
        setEventType("NOTIFICATION_DISPATCHED");
    }
}