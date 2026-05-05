package com.kafkaeventdriven.domain.repositories;

import com.kafkaeventdriven.domain.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}