package org.bobj.funding.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.funding.domain.FundingOrderVO;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundingOrderRequestDTO {
    @ApiModelProperty(value = "주문한 유저 ID")
    private Long userId;

    @ApiModelProperty(value = "펀딩 ID")
    private Long fundingId;

    @ApiModelProperty(value = "구매한 주 수")
    private Integer shareCount;

    public FundingOrderVO toVO(){
        return FundingOrderVO.builder()
                .userId(userId)
                .fundingId(fundingId)
                .shareCount(shareCount)
                .orderPrice(BigDecimal.valueOf(shareCount * 5000L))
                .build();
    }
}
