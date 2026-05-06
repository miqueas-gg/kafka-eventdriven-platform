package com.kafkaeventdriven.domain.exceptions; // <--- MUY IMPORTANTE

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}