package com.kafkaeventdriven.notification.repositories;

import com.kafkaeventdriven.notification.entities.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}