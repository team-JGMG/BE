package org.bobj.payment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVO {
    private Long paymentId;
    private Long userId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String pgTid;
    private String qrUrl;
    private LocalDateTime createdAt;
    private LocalDateTime endAt;
}
