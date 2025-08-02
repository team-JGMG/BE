package org.bobj.property.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.property.domain.PropertyVO;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyCreateDTO {
    @ApiModelProperty(value = "판매자(유저) ID", example = "1")
    private Long userId;
    @ApiModelProperty(value = "매물 제목", example = "강남 오피스텔 매물")
    private String title;
    @ApiModelProperty(value = "주소", example = "서울 강남구 테헤란로 123")
    private String address;

    @ApiModelProperty(value = "면적", example = "84.00")
    private BigDecimal area;
    @ApiModelProperty(value = "가격", example = "500000000")
    private BigDecimal price;
    @ApiModelProperty(value = "희망 공고 기간", example = "12")
    private Integer postingPeriod;

    @ApiModelProperty(value = "용도지역", example = "아파트")
    private String usageDistrict;
    @ApiModelProperty(value = "대지면적", example = "123.45")
    private Double landArea;
    @ApiModelProperty(value = "건물면적", example = "234.56")
    private Double buildingArea;
    @ApiModelProperty(value = "해당 매물 연면적", example = "200.00")
    private Double totalFloorAreaProperty;
    @ApiModelProperty(value = "건물 전체 연면적", example = "1000.00")
    private Double totalFloorAreaBuilding;

    @ApiModelProperty(value = "지하 층수", example = "1")
    private Integer basementFloors;
    @ApiModelProperty(value = "지상 층수", example = "15")
    private Integer groundFloors;

    @ApiModelProperty(value = "준공일", example = "2025-07-23")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate approvalDate;

    @ApiModelProperty(value = "공시지가", example = "400000000")
    private BigDecimal officialLandPrice;
    @ApiModelProperty(value = "평당가", example = "25000000")
    private BigDecimal unitPricePerPyeong;

    @ApiModelProperty(value = "부동산 유형", example = "오피스텔")
    private String propertyType;

    @ApiModelProperty(value = "방 개수", example = "3")
    private Integer roomCount;
    @ApiModelProperty(value = "욕실 개수", example = "2")
    private Integer bathroomCount;
    @ApiModelProperty(value = "층수", example = "10")
    private Integer floor;

    @ApiModelProperty(value = "설명", example = "역세권이며 투자 가치가 높습니다.")
    private String description;

    @ApiModelProperty(value ="법정동코드", example = "51743")
    private String rawdCd; //법정동코드, 사용자 입력 아닌 주소찾기 api 통해 사용.

    @ApiModelProperty(value ="임대수익", example = "5000000")
    private BigDecimal rentalIncome; //임대수익, 실거래가 api 통해 이용.

    public static PropertyCreateDTO of(PropertyVO vo){
        return PropertyCreateDTO.builder()
                .userId(vo.getUserId())
                .title(vo.getTitle())
                .address(vo.getAddress())
                .area(vo.getArea())
                .price(vo.getPrice())
                .postingPeriod(vo.getPostingPeriod())
                .usageDistrict(vo.getUsageDistrict())
                .landArea(vo.getLandArea() != null ? vo.getLandArea().doubleValue() : null)
                .buildingArea(vo.getBuildingArea() != null ? vo.getBuildingArea().doubleValue() : null)
                .totalFloorAreaProperty(vo.getTotalFloorAreaProperty() != null ? vo.getTotalFloorAreaProperty().doubleValue() : null)
                .totalFloorAreaBuilding(vo.getTotalFloorAreaBuilding() != null ? vo.getTotalFloorAreaBuilding().doubleValue() : null)
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
                .rawdCd(vo.getRawdCd())
                .rentalIncome(vo.getRentalIncome())
                .build();
    }

    public PropertyVO toVO() {
        return PropertyVO.builder()
                .userId(userId)
                .title(title)
                .address(address)
                .area(area)
                .price(price)
                .postingPeriod(postingPeriod)
                .usageDistrict(usageDistrict)
                .landArea(BigDecimal.valueOf(landArea))
                .buildingArea(BigDecimal.valueOf(buildingArea))
                .totalFloorAreaProperty(BigDecimal.valueOf(totalFloorAreaProperty))
                .totalFloorAreaBuilding(BigDecimal.valueOf(totalFloorAreaBuilding))
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
                .rawdCd(rawdCd)
                .rentalIncome(rentalIncome)
                .build();
    }
}
