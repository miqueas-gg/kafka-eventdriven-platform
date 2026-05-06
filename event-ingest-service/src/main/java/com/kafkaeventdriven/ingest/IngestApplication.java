package com.kafkaeventdriven.ingest; // Revisa que esta ruta coincida con tus carpetas

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
// Esto asegura que Spring encuentre tus servicios, entidades y consumidores
@ComponentScan(basePackages = "com.kafkaeventdriven") 
public class IngestApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestApplication.class, args);
    }
}