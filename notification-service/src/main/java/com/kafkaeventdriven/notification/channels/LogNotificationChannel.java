package com.kafkaeventdriven.notification.channels;

import com.kafkaeventdriven.events.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogNotificationChannel implements NotificationChannel {

    @Override
    public void dispatch(BaseEvent event, String recipient) {
        log.info("[CHANNEL - LOG] Procesando evento {} para: {}", 
            event.getClass().getSimpleName(), recipient);
    }

    @Override
    public String getChannelType() {
        return "LOG";
    }
}