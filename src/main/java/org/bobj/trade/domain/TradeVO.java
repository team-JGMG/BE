package org.bobj.trade.domain;

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
public class TradeVO {
    private Long tradeId;
    private Long buyOrderId;
    private Long sellOrderId;
    private Long buyerUserId;
    private Long sellerUserId;
    private Integer tradeCount;
    private BigDecimal tradePricePerShare;
    private LocalDateTime createdAt;
}
