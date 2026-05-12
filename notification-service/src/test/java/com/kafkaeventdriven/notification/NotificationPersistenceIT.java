package com.kafkaeventdriven.notification;

import com.kafkaeventdriven.notification.entities.NotificationEntity;
import com.kafkaeventdriven.notification.repositories.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest // Esto prueba solo la capa de datos con una DB en memoria (H2) por rapidez
class NotificationPersistenceIT {

    @Autowired
    private NotificationRepository repository;

    @Test
    void shouldSaveAndRetrieveNotification() {
        // GIVEN
        NotificationEntity notification = NotificationEntity.builder()
                .originalEventId(UUID.randomUUID())
                .eventType("ORDER_CREATED")
                .recipient("test@test.com")
                .status("SENT")
                .dispatchedAt(Instant.now())
                .build();

        // WHEN
        NotificationEntity saved = repository.save(notification);

        // THEN
        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }
}