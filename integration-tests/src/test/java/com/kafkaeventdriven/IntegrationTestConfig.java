package com.kafkaeventdriven;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.kafkaeventdriven"}) // Escanea la raíz de 
@EntityScan(basePackages = {"com.kafkaeventdriven"})    // Encuentra la EventEntity
@EnableJpaRepositories(basePackages = {"com.kafkaeventdriven"})
public class IntegrationTestConfig {
}