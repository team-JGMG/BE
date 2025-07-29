package org.bobj.funding.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.property.dto.PhotoDTO;
import org.bobj.property.dto.SellerDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundingDetailResponseDTO {
    @ApiModelProperty("펀딩 ID")
    private Long fundingId;

    @ApiModelProperty(value = "매물 ID")
    private Long propertyId;

    @ApiModelProperty("매물 제목")
    private String title;

    @ApiModelProperty("주소")
    private String address;

    @ApiModelProperty("목표 금액")
    private BigDecimal targetAmount;

    @ApiModelProperty("모집률 (%)")
    private Integer fundingRate;

    @ApiModelProperty("현재 모집 금액")
    private BigDecimal currentAmount;

    @ApiModelProperty(value = "펀딩 마감일", example = "2025-07-23")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fundingEndDate;

    @ApiModelProperty("펀딩 종료까지 남은 일 수")
    private long daysLeft;

    @ApiModelProperty("현재 주 당 가격")
    private BigDecimal currentShareAmount;

    // 건물 대장 정보
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

    @ApiModelProperty(value = "상세 설명", example = "역세권이며 투자 가치가 높습니다.")
    private String description;

    @ApiModelProperty(value = "사진 목록")
    private List<PhotoDTO> photos;

    @ApiModelProperty(value = "판매자 정보")
    private SellerDTO seller;
}
