package org.bobj.point.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointChargeRequestVO {

    private Long id;
    private Long userId;
    private BigDecimal amount; // 충전 금액
    private String merchantUid; // 포트원 결제 요청용
    private String impUid;      // 결제 성공 시 응답받는 UID
    private String status;      // PENDING, PAID, FAILED
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

}
