package com.example.HonBam.exception;

public class SnsAccessDeniedException extends RuntimeException {
    public SnsAccessDeniedException(String message) {
        super(message);
    }
}
