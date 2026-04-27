package com.kafkaeventdriven.domain.services;

import com.kafkaeventdriven.domain.dtos.ProductRequest;
import com.kafkaeventdriven.domain.entities.Product;
import com.kafkaeventdriven.events.ProductUpdatedEvent;
import com.kafkaeventdriven.domain.infrastructure.kafka.KafkaEventPublisher;
import com.kafkaeventdriven.domain.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final KafkaEventPublisher eventPublisher;
    private static final String TOPIC = "domain.events";

    @Transactional
    public Product createProduct(ProductRequest request) {
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .price(request.price())
                .stock(request.stock())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return productRepository.save(product);
    }

    public Product getProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    @Transactional
    public Product updateProduct(UUID id, ProductRequest request) {
        Product productEntity = getProduct(id); // La llamamos productEntity para no liarnos
        // Comparamos campos antes de actualizar
        checkAndPublishChange(productEntity, "name", productEntity.getName(), request.name());
        checkAndPublishChange(productEntity, "price", 
            String.valueOf(productEntity.getPrice()), 
            String.valueOf(request.price()));
        checkAndPublishChange(productEntity, "stock", 
            String.valueOf(productEntity.getStock()), 
            String.valueOf(request.stock()));

        // Actualizamos
        productEntity.setName(request.name());
        productEntity.setPrice(request.price());
        productEntity.setStock(request.stock());
        productEntity.setUpdatedAt(Instant.now());

        return productRepository.save(productEntity);
    }

    @Transactional
    public void updateStock(UUID id, Integer newStock) {
        Product productEntity = getProduct(id);
        String oldStock = String.valueOf(productEntity.getStock());
        
        productEntity.setStock(newStock);
        productEntity.setUpdatedAt(Instant.now());
        productRepository.save(productEntity);

        publishUpdate(productEntity, "stock", oldStock, String.valueOf(newStock));
    }

    private void checkAndPublishChange(Product p, String field, String oldVal, String newVal) {
        if (newVal != null && !newVal.equals(oldVal)) {
           publishUpdate(p, field, oldVal, newVal);
        }
    }

    private void publishUpdate(Product p, String field, String prev, String next) {
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(p.getId())
                .productName(p.getName())
                .changedField(field)
                .previousValue(prev)
                .newValue(next)
                .aggregateId(p.getId().toString())
                .source("domain-service")
                .eventType("PRODUCT_UPDATED")
                .correlationId(UUID.randomUUID())
                .build();
        
        eventPublisher.publish(event, TOPIC);
    }
    public Page<Product> getAllProducts(Pageable pageable) {
        log.info("Buscando página de productos: {}", pageable.getPageNumber());
        return productRepository.findAll(pageable);
    }
}