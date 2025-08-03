package org.bobj.funding.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FundingSoldResponseDTO {
    @ApiModelProperty(value = "펀딩 ID")
    private Long fundingId;
    @ApiModelProperty(value = "매물 ID")
    private Long propertyId;
}
