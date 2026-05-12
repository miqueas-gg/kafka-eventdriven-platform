package com.kafkaeventdriven.notification.channels;

import com.kafkaeventdriven.events.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationChannel implements NotificationChannel {

    @Override
    public void dispatch(BaseEvent event, String recipient) {
        log.info("[SIMULATED EMAIL] Enviando correo a -> {} | Contenido: Evento {} recibido correctamente.", 
            recipient, event.getClass().getSimpleName());
    }

    @Override
    public String getChannelType() {
        return "EMAIL";
    }
}