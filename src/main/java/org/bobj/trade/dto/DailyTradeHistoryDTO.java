package org.bobj.trade.dto;

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
@ApiModel(description = "일별 거래 내역 항목 DTO")
public class DailyTradeHistoryDTO {

    @ApiModelProperty(value = "날짜 (YYYY-MM-DD)", example = "2025-07-24", required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @ApiModelProperty(value = "해당 날짜의 종가", example = "5090.0", required = true)
    private BigDecimal closingPrice;

    @ApiModelProperty(value = "해당 날짜의 총 거래량", example = "163", required = true)
    private Integer volume;

    @ApiModelProperty(value = "이전 날짜 종가 대비 변화율 (%)", example = "-0.39", required = true)
    private BigDecimal priceChangeRate;
}
