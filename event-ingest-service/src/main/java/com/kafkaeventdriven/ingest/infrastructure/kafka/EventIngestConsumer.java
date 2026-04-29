package com.kafkaeventdriven.ingest.infrastructure.kafka;

import com.kafkaeventdriven.ingest.application.services.EventIngestService;
import com.kafkaeventdriven.ingest.domain.exceptions.InvalidEventException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry; // Inyectamos para las métricas

    @KafkaListener(topics = "domain.events", groupId = "event-ingest-group")
    public void consume(String message, Acknowledgment ack) {
        // Iniciamos el cronómetro para medir el tiempo de procesamiento
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            log.info("Mensaje recibido desde Kafka: {}", message);
            
            // MÉTRICA: Total recibidos
            meterRegistry.counter("ingest.events.received").increment();
            
            ingestService.processAndStore(message);
            
            // MÉTRICA: Persistidos con éxito
            meterRegistry.counter("ingest.events.persisted").increment();
            
            ack.acknowledge(); 
            
        } catch (InvalidEventException e) {
            // MÉTRICA: Inválidos
            meterRegistry.counter("ingest.events.invalid").increment();
            
            log.error("Evento inválido detectado. Enviando a DLT: {}", e.getMessage());
            sendToDlt(message, e.getMessage());
            ack.acknowledge();

        } catch (Exception e) {
            // MÉTRICA: Duplicados (si el mensaje de error lo indica)
            if (e.getMessage() != null && e.getMessage().contains("duplicado")) {
                meterRegistry.counter("ingest.events.duplicate").increment();
            }
            
            log.error("Error técnico (reintentable): {}", e.getMessage());
        } finally {
            // MÉTRICA: Tiempo de procesamiento (se registra siempre, pase lo que pase)
            sample.stop(meterRegistry.timer("ingest.processing.time"));
        }
    }

    private void sendToDlt(String payload, String reason) {
        ProducerRecord<String, String> record = new ProducerRecord<>("domain.events.dlt", payload);
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