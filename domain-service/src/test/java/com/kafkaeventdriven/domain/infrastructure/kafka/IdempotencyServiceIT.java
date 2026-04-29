package com.kafkaeventdriven.domain.infrastructure.kafka;

import com.kafkaeventdriven.domain.services.IdempotencyService;
import com.kafkaeventdriven.domain.repositories.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@ActiveProfiles("test")
class IdempotencyServiceIT {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private ProcessedEventRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldDetectDuplicateEvents() {
        UUID eventId = UUID.randomUUID();
        String type = "ORDER_CREATED";

        // 1. Primera vez: no existe
        assertThat(idempotencyService.isAlreadyProcessed(eventId)).isFalse();

        // 2. Marcamos como procesado
        idempotencyService.markAsProcessed(eventId, type);

        // 3. Segunda vez: ya existe
        assertThat(idempotencyService.isAlreadyProcessed(eventId)).isTrue();
    }

    @Test
    void shouldNotFailWhenMarkingAsProcessedTwice() {
        UUID eventId = UUID.randomUUID();
        String type = "TEST_TYPE";

        // 1. Primera vez: se guarda
        idempotencyService.markAsProcessed(eventId, type);
        assertThat(idempotencyService.isAlreadyProcessed(eventId)).isTrue();

        // 2. Segunda vez: no debe lanzar excepción (la Opción B que elegiste)
        // Simplemente llamamos y verificamos que no pasa nada malo
        idempotencyService.markAsProcessed(eventId, type);
        
        // 3. Verificamos que sigue existiendo y solo hay un registro
        assertThat(idempotencyService.isAlreadyProcessed(eventId)).isTrue();
        assertThat(repository.count()).isEqualTo(1); // Confirmamos que no se insertó duplicado
    }
}