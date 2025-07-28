package org.bobj.trade.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.trade.dto.DailyTradeHistoryDTO;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "펀딩 거래 내역 응답 DTO")
public class FundingTradeHistoryResponseDTO {
    @ApiModelProperty(value = "펀딩 ID", example = "1", required = true)
    private Long fundingId;

    @ApiModelProperty(value = "일별 거래 내역 목록 (날짜순 오름차순)", required = true)
    private List<DailyTradeHistoryDTO> history;
}
