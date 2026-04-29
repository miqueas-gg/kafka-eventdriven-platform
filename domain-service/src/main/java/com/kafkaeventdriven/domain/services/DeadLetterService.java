package com.kafkaeventdriven.domain.services;

import com.kafkaeventdriven.domain.entities.DeadLetterEvent;
import com.kafkaeventdriven.domain.repositories.DeadLetterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterService {

    private final DeadLetterRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Listar todos con paginación (para el GET)
    @Transactional(readOnly = true)
    public Page<DeadLetterEvent> getAllDeadLetters(Pageable pageable) {
        return repository.findAll(pageable);
    }

    // El famoso "Replay" (para el POST)
    @Transactional
    public void replayEvent(UUID id) {
        DeadLetterEvent dltEvent = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento DLT no encontrado con ID: " + id));

        log.info("Reintentando evento {} desde el DLT al topic {}", dltEvent.getEventId(), dltEvent.getOriginalTopic());

        // Volvemos a lanzar el payload original al topic que toca
        kafkaTemplate.send(dltEvent.getOriginalTopic(), dltEvent.getPayload());

        // Una vez reenviado, lo borramos de la tabla de fallos para que no estorbe
        repository.delete(dltEvent);
        
        log.info("Evento rebotado con éxito y eliminado de la tabla de auditoría");
    }
}
