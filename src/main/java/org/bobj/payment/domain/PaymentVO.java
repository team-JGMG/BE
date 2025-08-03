package org.bobj.payment.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentVO {
    private String impUid;
    private String merchantUid;
    private Long userId;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime paidAt;
//    private LocalDateTime createdAt;
}
