package org.bobj.funding.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundingVO {
    private Long fundingId;
    private Long propertyId;
    private Long currentAmount;
    private BigDecimal targetAmount;
    private BigDecimal currentShareAmount;
    private Integer totalShares;
    private Integer participantCount;
    private LocalDate fundingEndDate;
    private FundingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime endedAt;
}
