package org.bobj.funding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundingOrderLimitDTO {
    // 주 당 가격
    private BigDecimal ShareAmount;
    // 남은 주 수
    private Integer remainingShares;
    // 남은 금액
    private BigDecimal remainingAmount;
    // 유저가 보유한 주 수
    private Integer userShareCount;
    // 현재 유저가 보유한 포인트
    private BigDecimal userPoints;
}
