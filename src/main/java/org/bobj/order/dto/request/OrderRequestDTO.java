package org.bobj.order.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.order.domain.OrderVO;
import org.bobj.order.domain.OrderType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "거래 주문 등록 요청 DTO")
public class OrderRequestDTO {

    @ApiModelProperty(value = "펀딩 ID", example = "1", required = true)
    private Long fundingId;

    @ApiModelProperty(value = "주문 타입", example = "BUY", allowableValues = "BUY, SELL", required = true)
    private String orderType;

    @ApiModelProperty(value = "주당 주문 가격", example = "15000.00", required = true)
    private BigDecimal orderPricePerShare;

    @ApiModelProperty(value = "주문 수량", example = "10", required = true)
    private Integer orderShareCount;

    public OrderVO toVo() {
        return OrderVO.builder()
                .fundingId(fundingId)
                .orderType(OrderType.valueOf(orderType))
                .orderPricePerShare(orderPricePerShare)
                .orderShareCount(orderShareCount)
                .build();
    }
}
