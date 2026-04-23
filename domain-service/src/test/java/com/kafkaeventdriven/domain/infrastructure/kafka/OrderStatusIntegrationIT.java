package com.kafkaeventdriven.domain.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkaeventdriven.domain.dtos.UpdateStatusRequest;
import com.kafkaeventdriven.domain.entities.*;
import com.kafkaeventdriven.domain.repositories.CustomerRepository;
import com.kafkaeventdriven.domain.repositories.OrderRepository;
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
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EmbeddedKafka(partitions = 1, topics = {"domain.events"})
@ActiveProfiles("test")
class OrderStatusIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

    @KafkaListener(topics = "domain.events", groupId = "status-test-group")
    public void listen(String message) {
        messages.add(message);
    }

    private static final UUID CUSTOMER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        // Limpiamos Kafka y las tablas antes de cada test
        messages.clear();
        orderRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
    }
@Test
    void shouldPublishEventWhenStatusIsUpdatedSuccessfully() throws Exception {
        // 1. GIVEN
        Customer customer = Customer.builder()
                .name("Juan Pérez")
                .email("test@test.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        customer = customerRepository.saveAndFlush(customer);

        Order order = Order.builder()
                .customer(customer)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Order savedOrder = orderRepository.saveAndFlush(order);

        // Importante: "CONFIRMED" es válido desde "PENDING"
        UpdateStatusRequest request = new UpdateStatusRequest("CONFIRMED", "Pedido verificado");

        // 2. WHEN
        mockMvc.perform(patch("/api/orders/" + savedOrder.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent()); // Si esto pasa, el 204 funciona

        // 3. THEN: Verificamos Kafka (Subimos a 15s por si el EmbeddedKafka va lento)
        String receivedMessage = messages.poll(15, TimeUnit.SECONDS);
        
        assertThat(receivedMessage)
            .withFailMessage("El mensaje no llegó a Kafka. Revisa que el topic sea 'domain.events'")
            .isNotNull();
            
        assertThat(receivedMessage).contains("ORDER_STATUS_CHANGED");
        assertThat(receivedMessage).contains("CONFIRMED");
    }

   @Test
    void shouldNotPublishEventWhenTransitionIsInvalid() throws Exception {
        // 1. GIVEN: Creamos el cliente y un pedido ya CANCELADO
        Customer customer = Customer.builder()
                .name("Juan Pérez")
                .email("test@test.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        customer = customerRepository.saveAndFlush(customer);

        Order order = Order.builder()
                .customer(customer)
                .status(OrderStatus.CANCELLED) // Estado final
                .totalAmount(new BigDecimal("50.00"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Order savedOrder = orderRepository.saveAndFlush(order);

        UpdateStatusRequest request = new UpdateStatusRequest("CONFIRMED", "Intento inválido");

        // 2. WHEN & THEN: 
        // Usamos assertThatThrownBy o simplemente capturamos que MockMvc lanza la excepción
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            mockMvc.perform(patch("/api/orders/" + savedOrder.getId() + "/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }).hasCauseInstanceOf(IllegalArgumentException.class); // Verificamos que la causa sea tu validación

        // 3. FINALMENTE: Verificamos que Kafka sigue vacío
        String receivedMessage = messages.poll(2, TimeUnit.SECONDS);
        assertThat(receivedMessage).isNull(); 
    }
}
