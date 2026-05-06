package com.kafkaeventdriven.notification.infrastructure.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    /**
     * Este Bean gestiona qué pasa cuando el Listener (Consumer) falla.
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaOperations<Object, Object> template) {
        
        // 1. Configuramos el Backoff Exponencial
        // inicial: 1s, multiplicador: 2.0, maximo: 4s
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000L); // 1 segundo
        backOff.setMultiplier(2.0);        // 1s * 2 = 2s, 2s * 2 = 4s
        backOff.setMaxInterval(4000L);      // Límite de espera
        // Nota: El número de intentos se controla con el objeto BackOff o el ErrorHandler
        
        // 2. Definimos el Recoverer (A dónde va el mensaje tras fallar todos los intentos)
        // Por defecto, lo enviará al topic: originalTopic.DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);

        // 3. Creamos el ErrorHandler con 3 intentos en total (1 original + 2 reintentos)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        
        // Añadimos un log para ver cuándo actúa el ErrorHandler
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("Reintentando consumo de Kafka. Intento número: {}, Error: {}", 
                     deliveryAttempt, ex.getMessage());
        });

        return errorHandler;
    }
}