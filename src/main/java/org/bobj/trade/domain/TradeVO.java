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
    private Long orderId;
    private Long shareId;
    private Long userId;
    private TradeRole tradeRole; // BUYER,SELLER
    private Integer tradeCount;
    private BigDecimal tradePricePerShare;
    private LocalDateTime tradeDate;
}
