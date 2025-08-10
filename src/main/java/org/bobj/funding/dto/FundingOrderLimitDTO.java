package org.bobj.funding.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundingOrderLimitDTO {
    @ApiModelProperty("한 주당 가격")
    private BigDecimal shareAmount;
    @ApiModelProperty("현재 펀딩 구매 가능한 주 수(남아있는 주)")
    private Integer remainingShares;
    @ApiModelProperty("유저가 보유하고 있는 주 수")
    private Integer userShareCount;
    @ApiModelProperty("유저가 현재 보유하고 있는 포인트")
    private BigDecimal userPoints;
}
