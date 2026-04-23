package com.kafkaeventdriven.domain.controllers;

import com.kafkaeventdriven.domain.dtos.OrderRequest;
import com.kafkaeventdriven.domain.dtos.OrderResponse;
import com.kafkaeventdriven.domain.entities.OrderStatus;
import com.kafkaeventdriven.domain.services.OrderService;
import com.kafkaeventdriven.domain.dtos.UpdateStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // POST /api/orders -> Crear pedido
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // GET /api/orders/{id} -> Obtener por ID
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        // Nota: He asumido que añadirás este método simple al Service
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // GET /api/orders -> Listado paginado
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    // PATCH /api/orders/{id}/status -> Cambiar estado
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID id, 
            @RequestBody UpdateStatusRequest request){
        orderService.changeOrderStatus(id, request);
        return ResponseEntity.noContent().build(); // 204 No Content es estándar para actualizaciones exitosas
    }
}