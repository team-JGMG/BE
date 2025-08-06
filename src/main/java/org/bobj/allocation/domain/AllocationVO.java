package org.bobj.allocation.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationVO {
    
    private Long allocationsId;           // 배당금 고유 ID
    private Long fundingId;               // 펀딩 ID
    private BigDecimal dividendPerShare;  // 한 주 당 배당금액 (원)
    private BigDecimal totalDividendAmount; // 총 배당금액
    private LocalDate paymentDate;        // 배당 지급일
    private String paymentStatus;         // 배당 지급 상태 (PENDING, PROCESSING, COMPLETED, FAILED)
    private LocalDateTime createdAt;      // 생성일
    private LocalDateTime updatedAt;      // 수정일
}
