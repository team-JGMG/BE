package org.bobj.payment.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentVO {
    private Long paymentId;          // 선택: mapper에서 keyProperty 사용 시
    private String impUid;
    private String merchantUid;
    private Long userId;
    private BigDecimal amount;
    private PaymentStatus status;

    private LocalDateTime paidAt;        // 승인 시각
    private LocalDateTime canceledAt;    // 취소 시각
    private String pgProvider;           // PG사 //장애/정산/통계에서 “어느 PG/채널의 문제인가?” 추적 가능.

    private LocalDateTime lastEventAt;   // LWW 기준 시각
    private String lastEventSource;      // VERIFY/WEBHOOK
    private Long version;                // 낙관적 락

    private LocalDateTime createdAt;     // 선택(조회용)
    private LocalDateTime endAt;         // 선택(만료 관리)
}

