package org.bobj.share.domain;

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
public class ShareVO {
    private Long shareId;
    private Long userId;
    private Long fundingId;
    private int shareCount;
    private BigDecimal averageAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
