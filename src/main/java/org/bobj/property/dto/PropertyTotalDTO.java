package org.bobj.property.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.property.domain.PropertyStatus;
import org.bobj.property.domain.PropertyVO;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        PhotoDTO firstPhoto = null;
        if (vo.getPhotos() != null && !vo.getPhotos().isEmpty()) {
            firstPhoto = PhotoDTO.of(vo.getPhotos().get(0));
        }

        return PropertyTotalDTO.builder()
                .propertyId(vo.getPropertyId())
                .title(vo.getTitle())
                .address(vo.getAddress())
                .price(vo.getPrice())
                .status(vo.getStatus())
                .fundingStartDate(vo.getFundingStartDate())
                .fundingEndDate(vo.getFundingEndDate())
                .thumbnail(firstPhoto)
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
            builder.photos(java.util.List.of(this.thumbnail.toVO()));
        } else {
            builder.photos(java.util.Collections.emptyList());
        }

        return builder.build();
    }
}
