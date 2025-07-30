package org.bobj.property.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.property.domain.PropertyStatus;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyUserResponseDTO {
    @ApiModelProperty(value = "매물 ID", example = "1")
    private Long propertyId;
    @ApiModelProperty(value = "매물 제목", example = "강남 오피스텔")
    private String title;
    @ApiModelProperty(value = "희망 매매가", example = "750000000")
    private BigDecimal price;
    @ApiModelProperty(value = "매물 상태", example = "PENDING")
    private PropertyStatus status;

    @ApiModelProperty(value = "썸네일 이미지 정보")
    private PhotoDTO thumbnail;

    @ApiModelProperty(value = "펀딩 목표 금액")
    private BigDecimal targetAmount;
    @ApiModelProperty(value = "모집된 가격")
    private BigDecimal currentAmount;

    // 펀딩 관련 필드 (APPROVED, SOLD 시만 유효)
    @ApiModelProperty(value = "달성률")
    private Integer achievementRate;
    @ApiModelProperty(value = "남은 주 수")
    private Long remainingShares;
    @ApiModelProperty(value = "남은 금액")
    private BigDecimal remainingAmount;
}
