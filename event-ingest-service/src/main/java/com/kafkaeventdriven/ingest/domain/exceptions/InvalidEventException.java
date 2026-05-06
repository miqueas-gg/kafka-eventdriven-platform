package com.kafkaeventdriven.ingest.domain.exceptions;

public class InvalidEventException extends RuntimeException {
    public InvalidEventException(String message) {
        super(message);
    }
}