package org.bobj.allocation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "배당금 내역 응답 DTO")
public class AllocationResponseDTO {

    @ApiModelProperty(value = "배당금 고유 ID", example = "1", required = true)
    private Long allocationsId;

    @ApiModelProperty(value = "펀딩 ID", example = "100", required = true)
    private Long fundingId;

    @ApiModelProperty(value = "매물명", example = "강남 오피스텔", required = true)
    private String propertyTitle;

    @ApiModelProperty(value = "한 주 당 배당금액 (총 배당금 ÷ 총 주식 수)", example = "1350.00", required = true)
    private BigDecimal dividendPerShare;

    @ApiModelProperty(value = "총 배당금액 (임대수익의 90%, 10% 수수료 제외)", example = "135000.00", required = true)
    private BigDecimal totalDividendAmount;

    @ApiModelProperty(value = "총 주식 수", example = "100", required = true)
    private Integer totalShares;

    @ApiModelProperty(value = "배당 지급일", example = "2025-09-05", required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDate;

    @ApiModelProperty(value = "배당 지급 상태", example = "COMPLETED", allowableValues = "PENDING, PROCESSING, COMPLETED, FAILED", required = true)
    private String paymentStatus;

    @ApiModelProperty(value = "배당 지급 상태 한글명", example = "지급완료", required = true)
    private String paymentStatusKorean;

    // 상태 한글명 변환 메서드
    public String getPaymentStatusKorean() {
        if (paymentStatus == null) return "";
        
        switch (paymentStatus) {
            case "PENDING": return "지급예정";
            case "PROCESSING": return "지급중";
            case "COMPLETED": return "지급완료";
            case "FAILED": return "지급실패";
            default: return paymentStatus;
        }
    }
}
