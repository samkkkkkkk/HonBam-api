package com.example.HonBam.exception;

public class InvalidPaymentAmountException extends RuntimeException {
    public InvalidPaymentAmountException(String message) {
        super(message);
    }
}
