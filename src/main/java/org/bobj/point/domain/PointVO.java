package org.bobj.point.domain;

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
public class PointVO {
    private Long pointId;
    private Long userId;
    private BigDecimal amount;
    private LocalDateTime updatedAt;
}
