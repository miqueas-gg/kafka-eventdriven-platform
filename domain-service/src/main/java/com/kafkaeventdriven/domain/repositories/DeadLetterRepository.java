package com.kafkaeventdriven.domain.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kafkaeventdriven.domain.entities.DeadLetterEvent;

public interface DeadLetterRepository extends JpaRepository<DeadLetterEvent, UUID>{
    
}
