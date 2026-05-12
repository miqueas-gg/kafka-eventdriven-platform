package com.kafkaeventdriven.domain.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkaeventdriven.domain.dtos.OrderRequest;
import com.kafkaeventdriven.domain.dtos.OrderItemRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EmbeddedKafka(partitions = 1)
@ActiveProfiles("test")
@Sql(scripts = "/sql/data-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        // Necesitas IDs que existan si tu ddl-auto es validate
        // Para el test, puedes usar los que insertamos ayer manualmente en la DB
        UUID customerId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID productId = UUID.fromString("770e8400-e29b-41d4-a716-446655441111");

        OrderItemRequest item = new OrderItemRequest(productId, 2, new BigDecimal("25.50"));
        OrderRequest request = new OrderRequest(customerId, List.of(item), "Nota de test");

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // Verifica el 201
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(51.00));
    }
}