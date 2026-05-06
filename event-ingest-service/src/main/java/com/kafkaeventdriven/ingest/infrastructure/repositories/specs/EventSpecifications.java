package com.kafkaeventdriven.ingest.infrastructure.repositories.specs;

import com.kafkaeventdriven.ingest.domain.entities.EventEntity;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;

public class EventSpecifications {

    public static Specification<EventEntity> withFilters(String eventType, String source, LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (eventType != null && !eventType.isBlank()) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }

            if (source != null && !source.isBlank()) {
                predicates.add(cb.equal(root.get("source"), source));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}