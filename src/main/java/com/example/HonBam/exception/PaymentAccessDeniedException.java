package com.example.HonBam.exception;

public class PaymentAccessDeniedException extends RuntimeException {
    public PaymentAccessDeniedException(String message) {
        super(message);
    }
}
