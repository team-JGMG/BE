package org.bobj.funding.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.funding.domain.FundingVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundingDTO {
    @ApiModelProperty(value = "펀딩 ID")
    private Long fundingId;

    @ApiModelProperty(value = "매물 ID")
    private Long propertyId;

    @ApiModelProperty(value = "목표 금액")
    private BigDecimal targetAmount;

    @ApiModelProperty(value = "총 지분 수")
    private Integer totalShares;

    @ApiModelProperty(value = "펀딩 종료일")
    private LocalDateTime fundingEndDate;

    public static FundingDTO of(FundingVO vo){
        return FundingDTO.builder()
                .fundingId(vo.getFundingId())
                .propertyId(vo.getPropertyId())
                .targetAmount(vo.getTargetAmount())
                .totalShares(vo.getTotalShares())
                .fundingEndDate(vo.getFundingEndDate())
                .build();
    }
}
