package com.kafkaeventdriven.domain.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
// Puedes ponerlo en el mismo archivo o en uno separado
public record OrderItemRequest(
    @NotNull(message = "El producto es obligatorio")
    UUID productId,

    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    int quantity,

    @NotNull(message = "El precio unitario es obligatorio")
    BigDecimal unitPrice
) {}