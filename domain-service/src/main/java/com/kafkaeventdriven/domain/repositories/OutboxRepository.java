package com.kafkaeventdriven.domain.repositories;

import com.kafkaeventdriven.domain.entities.OutboxEvent; // Importa tu entidad
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    // Aquí puedes añadir métodos en el futuro, como buscar los no publicados
}