package org.bobj.order.domain;

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
public class OrderBookVO {
    private Long orderId;
    private Long userId;
    private Long fundingId;
    private OrderType orderType;                  // BUY or SELL
    private BigDecimal orderPricePerShare;
    private Integer orderShareCount;
    private OrderStatus status;                   // 'PENDING', 'PARTIALLY_FILLED', 'FULLY_FILLED', 'CANCELLED'
    private Integer remainingShareCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String propertyTitle;
}
