package com.kafkaeventdriven.domain.infrastructure.kafka;

import com.kafkaeventdriven.events.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${app.kafka.topic-name}")
    private String defaultTopic;

    public void publish(BaseEvent event, String topic) {
       try{
            String key = event.getAggregateId();
            String targetTopic = (topic != null) ? topic : defaultTopic;

            log.info("Publicando evento {} en tópico {}", event.getClass().getSimpleName(), targetTopic);

            kafkaTemplate.send(targetTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error al publicar en Kafka (Fire-and-forget): {}", ex.getMessage());
                    } else {
                        log.info("Evento publicado con éxito en offset {}", result.getRecordMetadata().offset());
                        Counter.builder("domain.events.published")
                            .description("Total de eventos publicados con éxito a Kafka")
                            .tag("event_type", event.getEventType())
                            .register(meterRegistry)
                            .increment();
                    }
                });
            }catch(Exception e){
                // Esto cumple el criterio de loguear pero no revertir BD
                log.error("Error crítico inmediato al publicar en Kafka: {}", e.getMessage());
            }
    }
}