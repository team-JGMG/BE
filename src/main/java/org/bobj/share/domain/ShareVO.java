package org.bobj.share.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareVO {
    private Long shareId;
    private Long userId;
    private Long fundingOrderId;
    private Long fundingId;
    private int shareCount;
    private Long averageAmount;
}
