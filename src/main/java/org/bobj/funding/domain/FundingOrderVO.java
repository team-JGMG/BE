package org.bobj.funding.domain;

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
public class FundingOrderVO {
    private Long orderId;
    private Long userId;
    private Long fundingId;
    private Integer shareCount;
    private FundingOrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal orderPrice;
}
