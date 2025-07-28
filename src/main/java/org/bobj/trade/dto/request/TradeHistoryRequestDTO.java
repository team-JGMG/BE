package org.bobj.trade.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "거래 내역 조회 요청 DTO")
public class TradeHistoryRequestDTO {
    @ApiModelProperty(value = "조회 시작일 (YYYY-MM-DD)", example = "2025-05-01", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @ApiModelProperty(value = "조회 종료일 (YYYY-MM-DD)", example = "2025-08-01", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @ApiModelProperty(value = "반환할 데이터 포인트 최대 개수 (페이징 시)", example = "60", required = false)
    private Integer limit;

    @ApiModelProperty(value = "페이지네이션 오프셋 (페이징 시)", example = "0", required = false)
    private Integer offset;
}
