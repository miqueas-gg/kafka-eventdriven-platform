package com.kafkaeventdriven.notification.channels;

import com.kafkaeventdriven.events.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

class WebhookNotificationChannelTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WebhookNotificationChannel webhookChannel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Inyectamos manualmente la URL que normalmente viene del application.yml
        ReflectionTestUtils.setField(webhookChannel, "webhookUrl", "http://test-url.com");
    }

    @Test
    void shouldSendPostRequestToWebhookUrl() {
        // Arrange
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .customerEmail("test@test.com")
                .build();

        // Act
        webhookChannel.dispatch(event, "test@test.com");

        // Assert
        // Verificamos que el restTemplate llamó a la URL correcta con el evento correcto
        verify(restTemplate, times(1)).postForEntity(
                eq("http://test-url.com"), 
                eq(event), 
                eq(Void.class)
        );
    }

    @Test
    void shouldReturnCorrectType() {
        assertEquals("WEBHOOK", webhookChannel.getChannelType());
    }
}