package com.kafkaeventdriven.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items", schema = "domain")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {

    @Id
    @GeneratedValue // El SQL usa gen_random_uuid()
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // CAMBIO CLAVE: En lugar de UUID productId, usamos la relación real
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    // DETALLE TÉCNICO: subtotal es GENERATED ALWAYS en el SQL
    // Le decimos a JPA que no lo incluya en los INSERT ni UPDATE
    @Column(insertable = false, updatable = false)
    private BigDecimal subtotal;
}