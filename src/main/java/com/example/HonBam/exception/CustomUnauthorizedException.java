package com.example.HonBam.exception;

public class CustomUnauthorizedException extends RuntimeException {
    public CustomUnauthorizedException(String message) {
        super(message);
    }
}
