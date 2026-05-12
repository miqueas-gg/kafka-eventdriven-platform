package com.kafkaeventdriven.ingest.application.services;

import com.kafkaeventdriven.ingest.application.dtos.EventResponseDTO;
import com.kafkaeventdriven.ingest.application.dtos.EventStatsDTO;
import com.kafkaeventdriven.ingest.domain.entities.EventEntity;
import com.kafkaeventdriven.ingest.domain.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true) // Optimizamos para solo lectura
public class EventQueryService {

    private final EventRepository eventRepository;

    // 1. Listar con filtros y paginación
    public Page<EventResponseDTO> findAll(Specification<EventEntity> spec, Pageable pageable) {
        log.info("Consultando eventos con filtros paginados");
        return eventRepository.findAll(spec, pageable)
                .map(this::mapToDTO);
    }

    // 2. Buscar por ID interno (el de la DB)
    public EventResponseDTO getById(UUID id) {
        return eventRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + id));
    }

    // 3. Buscar por EventID (el UUID de Kafka)
    public EventResponseDTO getByEventId(UUID eventId) {
        return eventRepository.findByEventId(eventId)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con UUID de negocio: " + eventId));
    }

    // 4. Obtener estadísticas por tipo
    public List<EventStatsDTO> getStats() {
        return eventRepository.getStatsByType();
    }

    // Método privado para convertir la Entidad en DTO (Mapeo manual)
    private EventResponseDTO mapToDTO(EventEntity entity) {
        return new EventResponseDTO(
                entity.getId(),
                entity.getEventId(),
                entity.getEventType(),
                entity.getSource(),
                entity.getPayload(),
                entity.getOccurredAt(),
                entity.getIngestedAt()
        );
    }
}