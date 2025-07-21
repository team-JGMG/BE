package org.bobj.property.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PropertyVO {
    private Long propertyId;
    private Long userId;

    // 기본 정보
    private String title;
    private String address;
    private String area;
    private BigDecimal price;
    private LocalDate fundingStartDate;
    private LocalDate fundingEndDate;
    private String status;

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
}
