package com.kafkaeventdriven.domain.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderTestConsumer {

    // Escucha el mismo topic donde publicas los cambios de estado
    @KafkaListener(topics = "domain.events", groupId = "test-group-fail")
    public void consumeAndFail(String message) {
        log.info("--- CONSUMER DE PRUEBA: Recibido mensaje para procesar ---");
        log.debug("Contenido: {}", message);
        
        // El sabotaje:
        throw new RuntimeException("¡BOOM! Error provocado para testear el DLT");
    }
}