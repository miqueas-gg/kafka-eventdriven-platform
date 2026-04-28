package com.kafkaeventdriven.domain.services;

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

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderRequest request) {
        // 1. Validar que el cliente existe
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + request.customerId()));

        // 2. Crear la cabecera del pedido (Status PENDING por defecto)
        Order order = Order.builder()
                .customer(customer)
                .status(OrderStatus.PENDING)
                .notes(request.notes())
                .build();

        // 3. Mapear y validar Items, y calcular el Total
        List<OrderItem> items = request.items().stream().map(itemRequest -> {
            // Validar que el producto existe (opcional, pero recomendado)
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

        // 4. Calcular el total acumulado
        BigDecimal total = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        order.setTotalAmount(total);

        // 5. Guardar en DB (Cascada guardará los items automáticamente)
        Order savedOrder = orderRepository.save(order);
        meterRegistry.counter("domain.orders.created").increment();
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId().toString(),
                savedOrder.getCustomer().getId(),
                savedOrder.getTotalAmount()
                );
                // 6. Publicar en Kafka dentro de la transacción
        try {
            eventPublisher.publish(event, null);
            log.debug("Publicación exitosa en Kafka para el pedido: {}", savedOrder.getId());
        } catch (Exception e) {
            log.error("Fallo al publicar en Kafka para el pedido: {}", savedOrder.getId(), e);
            // TODO: Limitación de consistencia. Aunque Kafka falle y lancemos excepción,
            // existe el riesgo de "Dual Write". Ver OUTBOX-1.
            
            throw new RuntimeException("Kafka Failure - Rolling back DB", e); // Lanzamos la excepción para forzar el ROLLBACK de la BD
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