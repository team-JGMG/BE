package org.bobj.property.dto;

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
public class PropertySoldResponseDTO {
    @ApiModelProperty(value = "매물 ID", example = "1")
    private Long propertyId;
    @ApiModelProperty(value = "썸네일 이미지 정보")
    private PhotoDTO thumbnail;
    @ApiModelProperty("매물 제목")
    private String title;
    @ApiModelProperty("누적 수익률")
    private BigDecimal cumulativeReturn;
}
