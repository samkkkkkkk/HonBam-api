package com.example.HonBam.exception;

import com.example.HonBam.exception.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
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

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePostNotFoundException(PostNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("POST_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFountException(CommentNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("COMMENT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidateRefreshTokenException(InvalidRefreshTokenException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_REFRESH_TOKEN", e.getMessage()));
    }

    // 기타 RuntimeException 처리 (fallback)
    // 도메인 검증성 예외는 전용 핸들러로 매핑되므로, 여기로 오면 예상치 못한 서버 오류로 간주한다.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled RuntimeException", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "예상치 못한 오류가 발생했습니다."));
    }

    // 모든 예외 처리 (가장 일반적인 예외)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex) {
        log.error("Unhandled Exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "예상치 못한 오류가 발생했습니다."));
    }
}
