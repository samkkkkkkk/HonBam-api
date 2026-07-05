package com.example.HonBam.paymentsapi.entity;

// Toss Payments 공식 결제 상태 (DB에는 Toss 응답 문자열이 그대로 저장됨)
public enum TossPaymentStatus {
    READY,
    IN_PROGRESS,
    WAITING_FOR_DEPOSIT,
    DONE,
    CANCELED,
    PARTIAL_CANCELED,
    ABORTED,
    EXPIRED
}
