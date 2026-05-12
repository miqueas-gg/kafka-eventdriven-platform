package com.kafkaeventdriven.domain.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkaeventdriven.domain.dtos.*;
import com.kafkaeventdriven.domain.entities.*;
import com.kafkaeventdriven.domain.exceptions.InvalidStateTransitionException;
import com.kafkaeventdriven.domain.infrastructure.kafka.KafkaEventPublisher;
import com.kafkaeventdriven.domain.repositories.*;
import com.kafkaeventdriven.events.OrderCreatedEvent;
import com.kafkaeventdriven.events.OrderStatusChangedEvent;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final KafkaEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper; // Para serializar el payload

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderRequest request) {
       
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + request.customerId()));

        Order order = Order.builder()
                .customer(customer)
                .status(OrderStatus.PENDING)
                .notes(request.notes())
                .build();

        
        List<OrderItem> items = request.items().stream().map(itemRequest -> {
           
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + itemRequest.productId()));

            return OrderItem.builder()
                    .order(order) // Vinculamos el item con el pedido
                    .product(product)
                    .quantity(itemRequest.quantity())
                    .unitPrice(itemRequest.unitPrice())
                    .build();
        }).toList();

        order.setItems(items);

        
        BigDecimal total = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        order.setTotalAmount(total);

        
        Order savedOrder = orderRepository.save(order);
        meterRegistry.counter("domain.orders.created").increment();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .aggregateId(savedOrder.getId().toString())
                .customerId(savedOrder.getCustomer().getId())
                .customerEmail(savedOrder.getCustomer().getEmail())
                .totalAmount(savedOrder.getTotalAmount())
                .eventId(UUID.randomUUID())
                .occurredAt(java.time.Instant.now())
                .source("domain-service")
                .build();
                
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            OutboxEvent outboxEntry = OutboxEvent.builder()
                    .id(event.getEventId()) // Usamos el mismo ID del evento
                    .aggregateType("ORDER")
                    .aggregateId(savedOrder.getId())
                    .eventType("ORDER_CREATED")
                    .payload(payload)
                    .createdAt(java.time.Instant.now())
                    .published(false) // Marcamos como NO publicado
                    .build();

            outboxRepository.save(outboxEntry); // Se guarda en la MISMA transacción que el Order
            log.info("Evento guardado en Outbox para el pedido: {}", savedOrder.getId());

        } catch (Exception e) {
            log.error("Fallo al guardar en Outbox", e);
            // Si esto falla, el throw forzará el ROLLBACK de la orden en la DB
            throw new RuntimeException("Fallo en persistencia de evento - Cancelando pedido", e);
        }
        return mapToResponse(savedOrder);
    }

    @Transactional
    public void updateStatus(UUID id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado"));

        validateTransition(order.getStatus(), newStatus);
        
        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        boolean isValid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            default -> false; // No se permite salir de SHIPPED o CANCELLED en este flujo
        };

        if (!isValid) {
            throw new InvalidStateTransitionException("No se puede pasar de " + current + " a " + next);
        }
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemDtos = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getProduct().getId(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                )).toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getNotes(),
                order.getCreatedAt(),
                itemDtos
        );
    }
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado"));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeOrderStatus(UUID orderId, UpdateStatusRequest request) {
        // 1. Buscamos el pedido
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        String previousStatus = order.getStatus().toString();
        
        // 2. Validación de lógica de negocio (Criterio de Aceptación)
        // Si falla aquí, lanza la excepción ANTES de tocar Kafka
        validateTransition(previousStatus, request.newStatus());

        // 3. Actualizamos en Base de Datos
        order.setStatus(OrderStatus.valueOf(request.newStatus()));
        orderRepository.save(order);
        Counter.builder("domain.orders.status_changed")
                        .tag("new_status", request.newStatus())
                        .register(meterRegistry)
                        .increment();

        // 4. Construimos el evento usando SuperBuilder
        // Al heredar de BaseEvent, puedes setear correlationId y source aquí mismo
        OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                .orderId(orderId)
                .previousStatus(previousStatus)
                .newStatus(request.newStatus())
                .reason(request.reason())
                .correlationId(UUID.randomUUID()) // Campo heredado
                .source("domain-service")                    // Campo heredado
                .eventType("ORDER_STATUS_CHANGED")           // Campo heredado
                .build();

        // 5. Publicamos
        try {
            eventPublisher.publish(event, "domain.events");
        } catch (Exception e) {
            log.error("Error al publicar cambio de estado", e);
            // Hacemos un wrap de la excepción para asegurar el Rollback de la BD
            throw new RuntimeException("Kafka failure - Rolling back status change", e);
        }
    }

    private void validateTransition(String current, String next) {
        // Ejemplo de validación: si el pedido está CANCELADO, no puedes moverlo a otro estado
        if ("CANCELLED".equals(current) || "COMPLETED".equals(current)) {
            throw new IllegalArgumentException("No se puede cambiar el estado de un pedido finalizado (" + current + ")");
        }
        
        // Validar que el nuevo estado sea válido (evita errores de Enum.valueOf)
        try {
            OrderStatus.valueOf(next);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de destino no válido: " + next);
        }
    }
}