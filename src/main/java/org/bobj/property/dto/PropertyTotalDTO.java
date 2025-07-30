package org.bobj.property.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.property.domain.PropertyStatus;
import org.bobj.property.domain.PropertyVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    @ApiModelProperty(value = "희망 공고 기간", example = "12")
    private Integer postingPeriod;
    @ApiModelProperty(value = "매물 상태", example = "PENDING")
    private PropertyStatus status;
    @ApiModelProperty(value = "등록일시", example = "2025-07-23T12:00:00")
    private LocalDateTime createdAt;

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
                .postingPeriod(vo.getPostingPeriod())
                .status(vo.getStatus())
                .createdAt(vo.getCreatedAt())

                .thumbnail(thumbnail)
                .build();
    }

    public PropertyVO toVO() {
        PropertyVO.PropertyVOBuilder builder = PropertyVO.builder()
                .propertyId(this.propertyId)
                .title(this.title)
                .address(this.address)
                .price(this.price)
                .postingPeriod(this.postingPeriod)
                .status(this.status)
                .createdAt(this.createdAt);

        if (this.thumbnail != null) {
            builder.photos(Collections.singletonList(this.thumbnail.toVO()));
        } else {
            builder.photos(Collections.emptyList());
        }

        return builder.build();
    }
}
