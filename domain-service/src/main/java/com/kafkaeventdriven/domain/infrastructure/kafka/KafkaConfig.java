package com.kafkaeventdriven.domain.infrastructure.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
        // 1. El "Recuperador": Si el mensaje falla, lo envía al topic original + ".dlt"
        // En tu caso, de "domain.events" lo mandará a "domain.events.dlt"
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);

        // 2. Política de reintentos: 
        // 1000L = espera 1 segundo entre intentos.
        // 2L = haz 2 reintentos (total 3 intentos: el original + 2 extras).
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }
}