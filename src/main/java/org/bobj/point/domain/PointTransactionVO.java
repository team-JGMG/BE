package org.bobj.point.domain;

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
public class PointTransactionVO {
    private Long pointTransactionId;
    private Long pointId;
    private PointTransactionType type; // ENUM('DEPOSIT', 'INVEST', ...) → 추후 enum으로 바꿔도 OK
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
