package com.kafkaeventdriven.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "notifications")
public class NotificationProperties {
    
    private String webhookUrl;
    
    /**
     * Este mapa recibirá los datos del YAML.
     * Clave: ORDER_CREATED, etc.
     * Valor: [LOG, EMAIL], etc.
     */
    private Map<String, List<String>> channels;
}