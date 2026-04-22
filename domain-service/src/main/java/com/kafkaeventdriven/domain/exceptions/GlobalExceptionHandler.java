package com.kafkaeventdriven.domain.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
// Maneja error 400 - Transiciones de estado (Lógica de negocio)
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<Map<String, String>> handleTransition(InvalidStateTransitionException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Transición inválida");
        error.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    // Maneja error 400 - Validaciones de campos (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    // Maneja error 404 - No encontrado (Cliente, Producto, Pedido)
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(jakarta.persistence.EntityNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Recurso no encontrado");
        error.put("message", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND); 
    }
}