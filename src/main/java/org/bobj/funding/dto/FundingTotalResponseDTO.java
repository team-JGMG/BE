package org.bobj.funding.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.property.dto.PhotoDTO;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundingTotalResponseDTO {
    @ApiModelProperty("펀딩 ID")
    private Long fundingId;

    @ApiModelProperty(value = "매물 ID")
    private Long propertyId;

    @ApiModelProperty("매물 제목")
    private String title;

    @ApiModelProperty("주소")
    private String address;

    @ApiModelProperty("목표 금액")
    private BigDecimal targetAmount;

    @ApiModelProperty("모집률 (%)")
    private Integer fundingRate;
    @ApiModelProperty("펀딩 종료까지 남은 일 수")
    private long daysLeft;

    @ApiModelProperty(value = "썸네일 이미지 정보")
    private PhotoDTO thumbnail;
}
