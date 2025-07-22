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

    private List<DocumentDTO> documents;
    private List<PhotoDTO> photos;

    public static PropertyDetailDTO of(PropertyVO vo) {
        return PropertyDetailDTO.builder()
                .propertyId(vo.getPropertyId())
                .userId(vo.getUserId())
                .seller(SellerDTO.of(vo.getSeller()))
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
                .documents(vo.getDocuments().stream().map(DocumentDTO::of).collect(Collectors.toList()))
                .photos(vo.getPhotos().stream().map(PhotoDTO::of).collect(Collectors.toList()))
                .build();
    }

    public PropertyVO toVO() {
        return PropertyVO.builder()
                .propertyId(propertyId)
                .userId(userId)
                .seller(seller.toVO())
                .title(title)
                .address(address)
                .area(area)
                .price(price)
                .fundingStartDate(fundingStartDate)
                .fundingEndDate(fundingEndDate)
                .status(status)
                .usageDistrict(usageDistrict)
                .landArea(landArea)
                .buildingArea(buildingArea)
                .totalFloorAreaProperty(totalFloorAreaProperty)
                .totalFloorAreaBuilding(totalFloorAreaBuilding)
                .basementFloors(basementFloors)
                .groundFloors(groundFloors)
                .approvalDate(approvalDate)
                .officialLandPrice(officialLandPrice)
                .unitPricePerPyeong(unitPricePerPyeong)
                .propertyType(propertyType)
                .roomCount(roomCount)
                .bathroomCount(bathroomCount)
                .floor(floor)
                .description(description)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .soldAt(soldAt)
                .documents(documents.stream().map(DocumentDTO::toVO).collect(Collectors.toList()))
                .photos(photos.stream().map(PhotoDTO::toVO).collect(Collectors.toList()))
                .build();
    }
}
