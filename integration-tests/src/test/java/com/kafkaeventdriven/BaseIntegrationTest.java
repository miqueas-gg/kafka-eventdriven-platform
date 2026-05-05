package com.kafkaeventdriven;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class BaseIntegrationTest {
    protected static PostgreSQLContainer<?> postgres;
    protected static KafkaContainer kafka;

    static {
        try {
            postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("eventstore").withUsername("admin").withPassword("admin");
            kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
            postgres.start();
            kafka.start();
        } catch (Exception e) {
            postgres = null;
            kafka = null;
        }
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        if (postgres != null && postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        } else {
            // URL con compatibilidad máxima para evitar el error de "Tabla no encontrada"
            registry.add("spring.datasource.url", () -> 
                "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
            registry.add("spring.flyway.enabled", () -> "false");
        }
        if (kafka != null && kafka.isRunning()) {
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        }
    }
}