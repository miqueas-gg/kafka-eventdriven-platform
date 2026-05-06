package com.kafkaeventdriven.ingest.application.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kafkaeventdriven.ingest.domain.exceptions.InvalidEventException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class EventValidatorTest {

    private EventValidator validator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        validator = new EventValidator();
        mapper = new ObjectMapper();
    }

    @Test
    void shouldPassWhenEventIsValid() {
        ObjectNode event = createValidEvent();
        assertThatCode(() -> validator.validate(event)).doesNotThrowAnyException();
    }

    @Test
    void shouldFailWhenEventIdIsMissing() {
        ObjectNode event = createValidEvent();
        event.remove("eventId");

        assertThatThrownBy(() -> validator.validate(event))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("eventId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldFailWhenEventTypeIsInvalid(String invalidType) {
        ObjectNode event = createValidEvent();
        event.put("eventType", invalidType);

        assertThatThrownBy(() -> validator.validate(event))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("eventType");
    }

    @Test
    void shouldFailWhenSourceIsMissing() {
        ObjectNode event = createValidEvent();
        event.set("source", null);

        assertThatThrownBy(() -> validator.validate(event))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("source");
    }

    @Test
    void shouldFailWhenVersionIsMissing() {
        ObjectNode event = createValidEvent();
        event.remove("version");

        assertThatThrownBy(() -> validator.validate(event))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("version");
    }

    @Test
    void shouldFailWhenDateIsInTheFuture() {
        ObjectNode event = createValidEvent();
        // Ponemos una fecha de mañana
        event.put("occurredAt", LocalDateTime.now().plusDays(1).toString());

        assertThatThrownBy(() -> validator.validate(event))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void shouldPassWhenDateIsWithinFiveMinuteTolerance() {
        ObjectNode event = createValidEvent();
        // 3 minutos en el futuro (dentro del margen de 5)
        event.put("occurredAt", LocalDateTime.now().plusMinutes(3).toString());

        assertThatCode(() -> validator.validate(event)).doesNotThrowAnyException();
    }

    // Helper para no repetir código
    private ObjectNode createValidEvent() {
        ObjectNode node = mapper.createObjectNode();
        node.put("eventId", "550e8400-e29b-41d4-a716-446655440000");
        node.put("eventType", "ORDER_CREATED");
        node.put("source", "domain-service");
        node.put("version", "v1");
        node.put("occurredAt", LocalDateTime.now().toString());
        return node;
    }
}