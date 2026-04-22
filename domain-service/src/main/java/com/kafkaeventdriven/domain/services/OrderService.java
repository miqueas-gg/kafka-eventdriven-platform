package com.kafkaeventdriven.domain.services;

import com.kafkaeventdriven.domain.dtos.*;
import com.kafkaeventdriven.domain.entities.*;
import com.kafkaeventdriven.domain.exceptions.InvalidStateTransitionException;
import com.kafkaeventdriven.domain.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Transactional
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
}