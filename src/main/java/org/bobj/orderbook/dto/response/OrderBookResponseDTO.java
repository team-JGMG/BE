package org.bobj.orderbook.dto.response;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.bobj.orderbook.dto.OrderBookEntryDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "호가창 응답 DTO")
public class OrderBookResponseDTO {

    @ApiModelProperty(value = "현재가", example = "5000.0", required = true)
    private BigDecimal currentPrice;

    @ApiModelProperty(value = "상한가", example = "6500.0", required = true)
    private BigDecimal upperLimitPrice;

    @ApiModelProperty(value = "하한가", example = "3500.0", required = true)
    private BigDecimal lowerLimitPrice;

    @ApiModelProperty(value = "매수 호가 리스트 (가격 내림차순)", required = true)
    private List<OrderBookEntryDTO> buyOrders;

    @ApiModelProperty(value = "매도 호가 리스트 (가격 오름차순)", required = true)
    private List<OrderBookEntryDTO> sellOrders;

    @ApiModelProperty(value = "호가창 기준 시간", example = "2025-07-25T10:00:00", required = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
