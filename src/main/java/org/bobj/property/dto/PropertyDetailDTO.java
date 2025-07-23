package org.bobj.property.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.property.domain.PropertyStatus;
import org.bobj.property.domain.PropertyVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyDetailDTO {
    private Long propertyId;
    private Long userId;

    private SellerDTO seller;

    // 기본 정보
    private String title;
    private String address;
    private String area;
    private BigDecimal price;
    private LocalDate fundingStartDate;
    private LocalDate fundingEndDate;
    private PropertyStatus status;

    // 건축 정보
    private String usageDistrict;
    private BigDecimal landArea;
    private BigDecimal buildingArea;
    private BigDecimal totalFloorAreaProperty;
    private BigDecimal totalFloorAreaBuilding;
    private Integer basementFloors;
    private Integer groundFloors;
    private LocalDate approvalDate;
    private BigDecimal officialLandPrice;
    private BigDecimal unitPricePerPyeong;

    // 상세 구조
    private String propertyType;
    private Integer roomCount;
    private Integer bathroomCount;
    private Integer floor;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime soldAt;

    // 첫 번째 사진을 썸네일으로
    private PhotoDTO thumbnail;

    private List<DocumentDTO> documents;
    private List<PhotoDTO> photos;

    public static PropertyDetailDTO of(PropertyVO vo) {
        PhotoDTO thumbnail = null;
        if (vo.getThumbnailUrl() != null) {
            thumbnail = PhotoDTO.builder()
                    .photoUrl(vo.getThumbnailUrl())
                    .build();
        }

        return PropertyDetailDTO.builder()
                .propertyId(vo.getPropertyId())
                .userId(vo.getUserId())
                .seller(vo.getSeller() != null ? SellerDTO.of(vo.getSeller()) : null)
                .title(vo.getTitle())
                .address(vo.getAddress())
                .area(vo.getArea())
                .price(vo.getPrice())
                .fundingStartDate(vo.getFundingStartDate())
                .fundingEndDate(vo.getFundingEndDate())
                .status(vo.getStatus())
                .usageDistrict(vo.getUsageDistrict())
                .landArea(vo.getLandArea())
                .buildingArea(vo.getBuildingArea())
                .totalFloorAreaProperty(vo.getTotalFloorAreaProperty())
                .totalFloorAreaBuilding(vo.getTotalFloorAreaBuilding())
                .basementFloors(vo.getBasementFloors())
                .groundFloors(vo.getGroundFloors())
                .approvalDate(vo.getApprovalDate())
                .officialLandPrice(vo.getOfficialLandPrice())
                .unitPricePerPyeong(vo.getUnitPricePerPyeong())
                .propertyType(vo.getPropertyType())
                .roomCount(vo.getRoomCount())
                .bathroomCount(vo.getBathroomCount())
                .floor(vo.getFloor())
                .description(vo.getDescription())
                .createdAt(vo.getCreatedAt())
                .updatedAt(vo.getUpdatedAt())
                .soldAt(vo.getSoldAt())
                .thumbnail(thumbnail)
                .documents(vo.getDocuments() != null
                        ? vo.getDocuments().stream().map(DocumentDTO::of).collect(Collectors.toList())
                        : List.of())
                .photos(vo.getPhotos() != null
                        ? vo.getPhotos().stream().map(PhotoDTO::of).collect(Collectors.toList())
                        : List.of())
                .build();
    }

    public PropertyVO toVO() {
        PropertyVO.PropertyVOBuilder builder = PropertyVO.builder()
                .propertyId(this.propertyId)
                .userId(this.userId)
                .seller(this.seller.toVO())
                .title(this.title)
                .address(this.address)
                .area(this.area)
                .price(this.price)
                .fundingStartDate(this.fundingStartDate)
                .fundingEndDate(this.fundingEndDate)
                .status(this.status)
                .usageDistrict(this.usageDistrict)
                .landArea(this.landArea)
                .buildingArea(this.buildingArea)
                .totalFloorAreaProperty(this.totalFloorAreaProperty)
                .totalFloorAreaBuilding(this.totalFloorAreaBuilding)
                .basementFloors(this.basementFloors)
                .groundFloors(this.groundFloors)
                .approvalDate(this.approvalDate)
                .officialLandPrice(this.officialLandPrice)
                .unitPricePerPyeong(this.unitPricePerPyeong)
                .propertyType(this.propertyType)
                .roomCount(this.roomCount)
                .bathroomCount(this.bathroomCount)
                .floor(this.floor)
                .description(this.description)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .soldAt(this.soldAt)
                .documents(this.documents.stream().map(DocumentDTO::toVO).collect(Collectors.toList()))
                .photos(this.photos.stream().map(PhotoDTO::toVO).collect(Collectors.toList()));
        if (this.thumbnail != null) {
            builder.photos(Collections.singletonList(this.thumbnail.toVO()));
        } else {
            builder.photos(Collections.emptyList());
        }

        return builder.build();
    }
}
