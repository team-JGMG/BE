package org.bobj.property.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PropertyVO {
    private Long propertyId;
    private Long userId;

    // 매도자 정보
    private SellerVO seller;

    // 기본 정보
    private String title;
    private String address;
    private BigDecimal area;
    private BigDecimal price;

    private Integer postingPeriod;

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

    private String rawdCd; //법정동코드

    private  BigDecimal rentalIncome;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime soldAt;

    // 문서 및 사진 리스트 추가
    private List<PropertyDocumentVO> documents;
    private List<PropertyPhotoVO> photos;

    // 썸네일 -> DB에는 없음!
    private String thumbnailUrl;

}
