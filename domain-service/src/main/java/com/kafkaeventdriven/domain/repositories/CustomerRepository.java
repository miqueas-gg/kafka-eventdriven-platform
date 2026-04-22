package com.kafkaeventdriven.domain.repositories;

import com.kafkaeventdriven.domain.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    // Un método útil para el futuro: buscar por email
    Optional<Customer> findByEmail(String email);
}