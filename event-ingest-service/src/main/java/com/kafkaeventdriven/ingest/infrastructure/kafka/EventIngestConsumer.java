package com.kafkaeventdriven.ingest.infrastructure.kafka;

import com.kafkaeventdriven.ingest.application.services.EventIngestService;
import com.kafkaeventdriven.ingest.domain.exceptions.InvalidEventException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventIngestConsumer {

    private final EventIngestService ingestService;
    private final KafkaTemplate<String, String> kafkaTemplate; // Para enviar al DLT

    @KafkaListener(topics = "domain.events", groupId = "event-ingest-group")
    public void consume(String message, Acknowledgment ack) {
        try {
            log.info("Mensaje recibido desde Kafka: {}", message);
            
            ingestService.processAndStore(message);
            
            // Si va bien, confirmamos
            ack.acknowledge(); 
            
        } catch (InvalidEventException e) {
            log.error("Evento inválido detectado. Enviando a DLT: {}", e.getMessage());
            sendToDlt(message, e.getMessage());
            
            // IMPORTANTE: Confirmamos el mensaje (ACK) aunque sea inválido.
            // ¿Por qué? Porque ya lo hemos gestionado enviándolo al DLT. 
            // Si no hiciéramos ACK, Kafka seguiría reintentando un mensaje que sabemos que está mal.
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error técnico (reintentable): {}", e.getMessage());
            // NO hacemos ack.acknowledge(). El mensaje se queda en Kafka para reintento técnico.
        }
    }

    private void sendToDlt(String payload, String reason) {
        ProducerRecord<String, String> record = new ProducerRecord<>("domain.events.dlt", payload);
        
        // Añadimos el header requerido: X-Error-Reason
        record.headers().add(new RecordHeader("X-Error-Reason", reason.getBytes(StandardCharsets.UTF_8)));
        
        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Fallo crítico: No se pudo enviar el mensaje al DLT", ex);
            } else {
                log.info("Mensaje enviado con éxito al DLT: domain.events.dlt");
            }
        });
    }
}