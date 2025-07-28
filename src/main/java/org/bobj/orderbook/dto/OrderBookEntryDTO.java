package org.bobj.orderbook.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "호가 단일 정보 DTO")
public class OrderBookEntryDTO {

    @ApiModelProperty(value = "호가 가격", example = "5000.0", required = true)
    private BigDecimal price;

    @ApiModelProperty(value = "수량", example = "10", required = true)
    private Integer quantity;
}
