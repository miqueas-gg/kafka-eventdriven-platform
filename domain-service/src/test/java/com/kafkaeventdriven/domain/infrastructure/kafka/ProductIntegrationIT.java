package com.kafkaeventdriven.domain.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkaeventdriven.domain.dtos.ProductRequest;
import com.kafkaeventdriven.domain.entities.Product;
import com.kafkaeventdriven.domain.entities.ProductStatus;
import com.kafkaeventdriven.domain.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EmbeddedKafka(partitions = 1, topics = {"domain.events"})
@ActiveProfiles("test")
class ProductIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

    @KafkaListener(topics = "domain.events", groupId = "product-test-group")
    public void listen(String message) {
        messages.add(message);
    }

    @BeforeEach
    void setUp() {
        messages.clear();
        productRepository.deleteAllInBatch();
    }

    @Test
    void shouldPublishMultipleEventsWhenMultipleFieldsAreUpdated() throws Exception {
        // 1. GIVEN: Un producto inicial
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name("Original Name")
                .price(new BigDecimal("10.00"))
                .stock(10)
                .status(ProductStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        product = productRepository.saveAndFlush(product);

        // 2. WHEN: Actualizamos los 3 campos (name, price, stock)
        ProductRequest updateRequest = new ProductRequest("New Name", new BigDecimal("20.00"), 50);

        mockMvc.perform(put("/api/products/" + product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // 3. THEN: Deben haber 3 eventos en Kafka
        // Esperamos un poco para que Kafka procese los 3
        Thread.sleep(1000); 
        
        assertThat(messages.size()).isEqualTo(3);
        
        String firstEvent = messages.poll();
        assertThat(firstEvent).contains("name").contains("Original Name").contains("New Name");
        
        String secondEvent = messages.poll();
        assertThat(secondEvent).contains("price").contains("10.00").contains("20.00");
        
        String thirdEvent = messages.poll();
        assertThat(thirdEvent).contains("stock").contains("10").contains("50");
    }

    @Test
    void shouldPublishSingleEventWhenOnlyStockIsUpdated() throws Exception {
        // 1. GIVEN
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name("Stock Product")
                .price(new BigDecimal("15.00"))
                .stock(100)
                .status(ProductStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        product = productRepository.saveAndFlush(product);

        // 2. WHEN: PATCH solo stock
        Map<String, Integer> stockUpdate = Map.of("stock", 200);

        mockMvc.perform(patch("/api/products/" + product.getId() + "/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockUpdate)))
                .andExpect(status().isNoContent());

        // 3. THEN: Solo 1 evento
        String event = messages.poll(10, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event).contains("stock").contains("100").contains("200");
        assertThat(messages).isEmpty(); // Verificamos que no hay más
    }
}