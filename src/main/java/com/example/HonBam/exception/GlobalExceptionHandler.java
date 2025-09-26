package com.example.HonBam.exception;

import com.example.HonBam.exception.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ChatRoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChatRoomNotFoundException(ChatRoomNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("CHAT_ROOM_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ChatRoomAccessException.class)
    public ResponseEntity<ErrorResponse> handleChatRoomAccessException(ChatRoomAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("CHAT_ROOM_ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(ChatRoomValidationException.class)
    public ResponseEntity<ErrorResponse> handleChatRoomValidationException(ChatRoomValidationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("CHAT_ROOM_NOT_VALIDATED", ex.getMessage()));
    }

    @ExceptionHandler(MessageSendException.class)
    public ResponseEntity<ErrorResponse> handleMessageSendException(MessageSendException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("MESSAGE_SEND_ERROR", ex.getMessage()));
    }

    // 기타 RuntimeException 처리 (fallback)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {

        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // 모든 예외 처리 (가장 일반적인 예외)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex) {
        return new ResponseEntity<>("An unexpected error occurred: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
