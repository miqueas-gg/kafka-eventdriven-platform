package com.kafkaeventdriven.ingest.domain.repositories;

import com.kafkaeventdriven.ingest.application.dtos.EventStatsDTO;
import com.kafkaeventdriven.ingest.domain.entities.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, UUID>, JpaSpecificationExecutor<EventEntity> {
// Mantener para Idempotencia (Issue anterior)
    boolean existsByEventId(UUID eventId);

    // Nuevo: Buscar por el UUID de negocio
    Optional<EventEntity> findByEventId(UUID eventId);

    // Nuevo: Estadísticas agregadas
    @Query("SELECT new com.kafkaeventdriven.ingest.application.dtos.EventStatsDTO(e.eventType, COUNT(e)) " +
           "FROM EventEntity e " +
           "GROUP BY e.eventType")
    List<EventStatsDTO> getStatsByType();
}