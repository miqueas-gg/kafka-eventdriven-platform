package com.kafkaeventdriven.ingest.domain.repositories;

import com.kafkaeventdriven.ingest.domain.entities.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, UUID> {
    
    boolean existsByEventId(UUID eventId);
}