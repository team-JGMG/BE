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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyDetailDTO {
    @ApiModelProperty(value = "매물 ID", example = "1")
    private Long propertyId;
    @ApiModelProperty(value = "등록자 유저 ID", example = "1")
    private Long userId;

    @ApiModelProperty(value = "판매자 정보")
    private SellerDTO seller;

    // 기본 정보
    @ApiModelProperty(value = "매물 제목", example = "강남 오피스텔")
    private String title;
    @ApiModelProperty(value = "주소", example = "서울 강남구 테헤란로 123")
    private String address;
    @ApiModelProperty(value = "면적", example = "84.00")
    private BigDecimal area;
    @ApiModelProperty(value = "희망 매매가", example = "750000000")
    private BigDecimal price;
    @ApiModelProperty(value = "희망 공고 기간", example = "12")
    private Integer postingPeriod;
    @ApiModelProperty(value = "매물 상태", example = "PENDING")
    private PropertyStatus status;

    // 건축 정보
    @ApiModelProperty(value = "용도지역", example = "제2종 일반주거지역")
    private String usageDistrict;
    @ApiModelProperty(value = "대지면적(m²)", example = "123.45")
    private BigDecimal landArea;
    @ApiModelProperty(value = "건물면적(m²)", example = "234.56")
    private BigDecimal buildingArea;
    @ApiModelProperty(value = "해당 매물 연면적(m²)", example = "200.00")
    private BigDecimal totalFloorAreaProperty;
    @ApiModelProperty(value = "건물 전체 연면적(m²)", example = "1000.00")
    private BigDecimal totalFloorAreaBuilding;
    @ApiModelProperty(value = "지하 층수", example = "1")
    private Integer basementFloors;
    @ApiModelProperty(value = "지상 층수", example = "15")
    private Integer groundFloors;
    @ApiModelProperty(value = "준공일", example = "2025-07-23")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate approvalDate;
    @ApiModelProperty(value = "공시지가(₩)", example = "400000000")
    private BigDecimal officialLandPrice;
    @ApiModelProperty(value = "평당가(₩)", example = "25000000")
    private BigDecimal unitPricePerPyeong;

    // 상세 구조
    @ApiModelProperty(value = "부동산 유형", example = "오피스텔")
    private String propertyType;
    @ApiModelProperty(value = "방 개수", example = "3")
    private Integer roomCount;
    @ApiModelProperty(value = "욕실 개수", example = "2")
    private Integer bathroomCount;
    @ApiModelProperty(value = "층수", example = "10")
    private Integer floor;
    @ApiModelProperty(value = "상세 설명", example = "역세권이며 투자 가치가 높습니다.")
    private String description;
    @ApiModelProperty(value = "등록일시", example = "2025-07-23T12:00:00")
    private LocalDateTime createdAt;
    @ApiModelProperty(value = "수정일시", example = "2025-07-24T09:30:00")
    private LocalDateTime updatedAt;
    @ApiModelProperty(value = "판매완료일시", example = "2025-08-01T17:00:00")
    private LocalDateTime soldAt;

    // 첫 번째 사진을 썸네일으로
    @ApiModelProperty(value = "썸네일 이미지 정보")
    private PhotoDTO thumbnail;

    @ApiModelProperty(value = "서류 파일 목록")
    private List<DocumentDTO> documents;
    @ApiModelProperty(value = "사진 목록")
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
                .postingPeriod(vo.getPostingPeriod())
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
                .postingPeriod(postingPeriod)
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
