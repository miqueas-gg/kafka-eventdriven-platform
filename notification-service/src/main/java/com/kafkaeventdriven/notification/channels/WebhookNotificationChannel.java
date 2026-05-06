package com.kafkaeventdriven.notification.channels;

import com.kafkaeventdriven.events.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookNotificationChannel implements NotificationChannel {

    private final RestTemplate restTemplate;

    // La URL se leerá de application.yml, con un default por si acaso
    @Value("${notifications.webhook-url:http://localhost:8080/callback}")
    private String webhookUrl;

    @Override
    public void dispatch(BaseEvent event, String recipient) {
        try {
            log.info("[CHANNEL - WEBHOOK] Enviando POST a {} para el evento {}", 
                webhookUrl, event.getEventId());
            
            // Enviamos el objeto evento completo como JSON en el cuerpo del POST
            restTemplate.postForEntity(webhookUrl, event, Void.class);
            
        } catch (Exception e) {
            log.error("[CHANNEL - WEBHOOK] Error al llamar al webhook: {}", e.getMessage());
            // Lanzamos la excepción para que el Dispatcher sepa que este canal falló
            throw e; 
        }
    }

    @Override
    public String getChannelType() {
        return "WEBHOOK";
    }
}