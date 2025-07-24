package org.bobj.share.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.share.domain.ShareVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "지분 조회 응답 DTO")
public class ShareResponseDTO {

    @ApiModelProperty(value = "지분 ID", example = "1", required = true)
    private Long shareId;

    @ApiModelProperty(value = "사용자 ID", example = "1", required = true)
    private Long userId;

    @ApiModelProperty(value = "펀딩 ID", example = "1", required = true)
    private Long fundingId;

    @ApiModelProperty(value = "썸네일 URL", example = "http://example.com/property_photos/abc.jpg", required = false)
    private String thumbnailUrl;

    @ApiModelProperty(value = "건물 이름", example = "강남센트럴아이파크", required = true)
    private String propertyTitle;

    @ApiModelProperty(value = "현재 시세", example = "5550", required = true)
    private BigDecimal currentShareAmount;

    @ApiModelProperty(value = "주식 수량", example = "10", required = true)
    private Integer shareCount;

    @ApiModelProperty(value = "평단가", example = "5000", required = true)
    private BigDecimal averageAmount;

    public static ShareResponseDTO of(ShareVO shareVO) {
        return ShareResponseDTO.builder()
                .shareId(shareVO.getShareId())
                .userId(shareVO.getUserId())
                .fundingId(shareVO.getFundingId())
                .shareCount(shareVO.getShareCount())
                .averageAmount(shareVO.getAverageAmount())
                .build();
    }
}
