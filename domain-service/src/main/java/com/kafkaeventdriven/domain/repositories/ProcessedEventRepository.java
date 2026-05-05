package com.kafkaeventdriven.domain.repositories;

import com.kafkaeventdriven.domain.entities.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}