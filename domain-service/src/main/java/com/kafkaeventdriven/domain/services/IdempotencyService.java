package com.kafkaeventdriven.domain.services;

import com.kafkaeventdriven.domain.entities.ProcessedEvent;
import com.kafkaeventdriven.domain.repositories.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository repository;

    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(UUID eventId) {
        return repository.existsById(eventId);
    }

    @Transactional
    public void markAsProcessed(UUID eventId, String eventType) {
        if (repository.existsById(eventId)) {
        log.info("El evento {} ya estaba marcado como procesado. Ignorando...", eventId);
        return;
        }
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .processedAt(Instant.now())
                .build();
        repository.save(processedEvent);
        log.info("Evento {} de tipo {} marcado como procesado.", eventId, eventType);
    }
}