package org.bobj.property.dto;

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
    private Long propertyId;
    private String title;
    private String address;
    private BigDecimal price;
    private PropertyStatus status;
    private LocalDate fundingStartDate;
    private LocalDate fundingEndDate;

    // 첫 번째 사진을 썸네일으로
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
