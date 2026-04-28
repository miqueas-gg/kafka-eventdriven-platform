package com.kafkaeventdriven.ingest.infrastructure.kafka;

import com.kafkaeventdriven.ingest.application.services.EventIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventIngestConsumer {

    private final EventIngestService ingestService;

    // Escuchamos el topic y el grupo que pidió el tutor
    @KafkaListener(topics = "domain.events", groupId = "event-ingest-group")
    public void consume(String message, Acknowledgment ack) {
        try {
            log.info("Mensaje recibido desde Kafka: {}", message);
            
            // Llamamos al servicio para validar y guardar
            ingestService.processAndStore(message);
            
            // REQUISITO TÉCNICO: Si fue bien, confirmamos manualmente
            ack.acknowledge(); 
            
        } catch (Exception e) {
            log.error("Error procesando mensaje. No se enviará el ACK, permitiendo reintentos: {}", e.getMessage());
            // Al NO llamar a ack.acknowledge(), Spring Kafka sabe que debe reintentar
            // según la configuración de reintentos que pongamos luego.
        }
    }
}