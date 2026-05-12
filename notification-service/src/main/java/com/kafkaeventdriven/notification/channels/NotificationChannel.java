package com.kafkaeventdriven.notification.channels;

import com.kafkaeventdriven.events.BaseEvent;

public interface NotificationChannel {
    /**
     * Envía la notificación
     * @param event El evento original (OrderCreated, etc)
     * @param recipient El destinatario (email o URL)
     */
    void dispatch(BaseEvent event, String recipient);

    /**
     * Devuelve el nombre del canal (ej: "LOG", "EMAIL", "WEBHOOK")
     */
    String getChannelType();
}