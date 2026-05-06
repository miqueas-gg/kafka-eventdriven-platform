package com.kafkaeventdriven.notification.channels;

import com.kafkaeventdriven.events.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SimpleChannelsTest {

    @Test
    void testLogChannel() {
        LogNotificationChannel channel = new LogNotificationChannel();
        OrderCreatedEvent event = OrderCreatedEvent.builder().eventId(UUID.randomUUID()).build();
        assertDoesNotThrow(() -> channel.dispatch(event, "test@test.com"));
    }

    @Test
    void testEmailChannel() {
        EmailNotificationChannel channel = new EmailNotificationChannel();
        OrderCreatedEvent event = OrderCreatedEvent.builder().eventId(UUID.randomUUID()).build();
        assertDoesNotThrow(() -> channel.dispatch(event, "test@test.com"));
    }
}