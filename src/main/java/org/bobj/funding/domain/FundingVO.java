package org.bobj.funding.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundingVO {
    private Long fundingId;
    private Long propertyId;
    private BigDecimal currentAmount;
    private BigDecimal targetAmount;
    private BigDecimal currentShareAmount;
    private Integer totalShares;

    private FundingStatus status;
    private LocalDateTime fundingStartDate;
    private LocalDateTime fundingEndDate;

    // 남은 주 수 계산
    public int getRemainingShares() {
        BigDecimal sharePrice = BigDecimal.valueOf(5000);

        int currentShares = currentAmount != null
                ? currentAmount.divide(sharePrice, 0, RoundingMode.DOWN).intValue()
                : 0;

        return totalShares - currentShares;
    }
}
