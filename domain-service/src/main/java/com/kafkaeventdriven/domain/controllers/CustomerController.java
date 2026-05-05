package com.kafkaeventdriven.domain.controllers;

import com.kafkaeventdriven.domain.entities.Customer;
import com.kafkaeventdriven.domain.services.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {
    
    private final CustomerService customerService;

    @GetMapping
    public List<Customer> getAll() {
        return customerService.getAllCustomers();
    }

    @PostMapping
    public Customer create(@RequestBody Customer customer) {
        return customerService.save(customer);
    }
}