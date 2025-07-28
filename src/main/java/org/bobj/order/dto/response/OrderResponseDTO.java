package org.bobj.order.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.order.domain.OrderVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "거래 주문 응답 DTO")
public class OrderResponseDTO {

    @ApiModelProperty(value = "주문 ID", example = "1", required = false)
    private Long orderId;

    @ApiModelProperty(value = "사용자 ID", example = "1", required = true)
    private Long userId;

    @ApiModelProperty(value = "펀딩 ID", example = "1", required = true)
    private Long fundingId;

    @ApiModelProperty(value = "건물 이름", example = "강남센트럴아이파크", required = false)
    private String propertyTitle;

    @ApiModelProperty(value = "주문 타입 (BUY 또는 SELL)", example = "BUY", required = true, allowableValues = "BUY, SELL")
    private String orderType;

    @ApiModelProperty(value = "주당 주문 가격", example = "15000.00", required = true)
    private BigDecimal orderPricePerShare;

    @ApiModelProperty(value = "주문 수량", example = "10", required = true)
    private Integer orderShareCount;

    @ApiModelProperty(value = "주문 상태 (기본값: PENDING)", example = "PENDING", required = false, allowableValues = "PENDING,PARTIALLY_FILLED,FULLY_FILLED,CANCELLED")
    private String status;

    @ApiModelProperty(value = "남은 수량", example = "10", required = false)
    private Integer remainingShareCount;

    @ApiModelProperty(value = "등록 일시", example = "2025-07-20T15:30:00", required = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "수정 일시", example = "2025-07-20T15:30:00", required = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static OrderResponseDTO of(OrderVO vo){
        return vo == null? null : OrderResponseDTO.builder()
                .orderId(vo.getOrderId())
                .userId(vo.getUserId())
                .fundingId(vo.getFundingId())
                .propertyTitle(vo.getPropertyTitle())
                .orderType(vo.getOrderType().name())
                .orderPricePerShare(vo.getOrderPricePerShare())
                .orderShareCount(vo.getOrderShareCount())
                .status(vo.getStatus().name())
                .remainingShareCount(vo.getRemainingShareCount())
                .createdAt(vo.getCreatedAt())
                .updatedAt(vo.getUpdatedAt())
                .build();
    }
}
