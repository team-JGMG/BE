package org.bobj.order.domain;

public enum OrderStatus {
    PENDING,             // 주문만 등록된 상태 (체결 전)
    PARTIALLY_FILLED,    // 일부 체결됨
    FULLY_FILLED,        // 전량 체결됨 (기존 MATCHED)
    CANCELLED            // 취소됨
}