package com.kafkaeventdriven.domain.controllers;

import com.kafkaeventdriven.domain.dtos.ProductRequest;
import com.kafkaeventdriven.domain.entities.Product;
import com.kafkaeventdriven.domain.services.ProductService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping
    public ResponseEntity<Page<Product>> getAll(
        @org.springframework.data.web.PageableDefault(size = 10, sort = "name") org.springframework.data.domain.Pageable pageable
    ) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }   

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable UUID id, @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Void> updateStock(@PathVariable UUID id, @RequestBody Map<String, Integer> body) {
        productService.updateStock(id, body.get("stock"));
        return ResponseEntity.noContent().build();
    }
}