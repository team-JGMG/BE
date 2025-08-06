package org.bobj.allocation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 배당 지급용 데이터 전송 객체
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DividendPaymentDTO {
    
    private Long userId;              // 사용자 ID
    private String userName;          // 사용자 이름
    private Integer shareCount;       // 보유 주식 수
    private BigDecimal dividendPerShare;  // 주당 배당금
    private BigDecimal totalDividend;     // 총 받을 배당금 (주식 수 * 주당 배당금)
    private Long fundingId;           // 펀딩 ID
    
    // 총 배당금 계산 메서드
    public BigDecimal calculateTotalDividend() {
        if (shareCount != null && dividendPerShare != null) {
            this.totalDividend = dividendPerShare.multiply(BigDecimal.valueOf(shareCount));
            return this.totalDividend;
        }
        return BigDecimal.ZERO;
    }
}
