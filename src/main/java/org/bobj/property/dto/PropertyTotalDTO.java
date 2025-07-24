package org.bobj.property.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.property.domain.PropertyStatus;
import org.bobj.property.domain.PropertyVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyTotalDTO {
    @ApiModelProperty(value = "매물 ID", example = "1")
    private Long propertyId;
    @ApiModelProperty(value = "매물 제목", example = "강남 오피스텔")
    private String title;
    @ApiModelProperty(value = "주소", example = "서울 강남구 테헤란로 123")
    private String address;
    @ApiModelProperty(value = "희망 매매가", example = "750000000")
    private BigDecimal price;
    @ApiModelProperty(value = "매물 상태", example = "PENDING")
    private PropertyStatus status;
    @ApiModelProperty(value = "펀딩 시작일", example = "2025-07-23")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fundingStartDate;
    @ApiModelProperty(value = "펀딩 종료일", example = "2025-08-23")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fundingEndDate;

    // 첫 번째 사진을 썸네일으로
    @ApiModelProperty(value = "썸네일 이미지 정보")
    private PhotoDTO thumbnail;

    public static PropertyTotalDTO of(PropertyVO vo) {
        PhotoDTO thumbnail = null;
        if (vo.getThumbnailUrl() != null) {
            thumbnail = PhotoDTO.builder()
                    .photoUrl(vo.getThumbnailUrl())
                    .build();
        }

        return PropertyTotalDTO.builder()
                .propertyId(vo.getPropertyId())
                .title(vo.getTitle())
                .address(vo.getAddress())
                .price(vo.getPrice())
                .status(vo.getStatus())
                .fundingStartDate(vo.getFundingStartDate())
                .fundingEndDate(vo.getFundingEndDate())
                .thumbnail(thumbnail)
                .build();
    }

    public PropertyVO toVO() {
        PropertyVO.PropertyVOBuilder builder = PropertyVO.builder()
                .propertyId(this.propertyId)
                .title(this.title)
                .address(this.address)
                .price(this.price)
                .status(this.status)
                .fundingStartDate(this.fundingStartDate)
                .fundingEndDate(this.fundingEndDate);

        if (this.thumbnail != null) {
            builder.photos(Collections.singletonList(this.thumbnail.toVO()));
        } else {
            builder.photos(Collections.emptyList());
        }

        return builder.build();
    }
}
