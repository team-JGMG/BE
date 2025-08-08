package org.bobj.funding.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.trade.dto.request.TradeHistoryRequestDTO;
import org.bobj.trade.dto.response.FundingTradeHistoryResponseDTO;
import org.bobj.trade.service.TradeHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fundings")
@RequiredArgsConstructor
@Log4j2
@Api(tags="펀딩 히스토리 API")
public class FundingHistoryController {
    private final TradeHistoryService tradeHistoryService;

    @GetMapping("/{fundingId}/trades")
    @ApiOperation(value = "펀딩 일별 거래 내역 조회", notes = "특정 펀딩의 일별 종가, 거래량 및 변화율을 조회합니다. (daily 고정)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "fundingId", value = "조회할 펀딩 ID", required = true, dataType = "long", paramType = "path", example = "1"),
            @ApiImplicitParam(name = "startDate", value = "조회 시작일 (YYYY-MM-DD)", required = false, dataType = "string", paramType = "query", example = "2025-06-01"),
            @ApiImplicitParam(name = "endDate", value = "조회 종료일 (YYYY-MM-DD)", required = false, dataType = "string", paramType = "query", example = "2025-06-30"),
            @ApiImplicitParam(name = "limit", value = "반환할 데이터 포인트 최대 개수", required = false, dataType = "int", paramType = "query", example = "100"),
            @ApiImplicitParam(name = "offset", value = "페이지네이션 오프셋", required = false, dataType = "int", paramType = "query", example = "0")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "거래 내역 조회 성공", response = FundingTradeHistoryResponseDTO.class),
            @ApiResponse(code = 400, message = "잘못된 요청 파라미터", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "펀딩을 찾을 수 없음", response = ErrorResponse.class), // 펀딩 ID가 유효하지 않을 경우 등
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<FundingTradeHistoryResponseDTO>> getFundingDailyTradeHistory(
            @PathVariable Long fundingId,
            @ModelAttribute TradeHistoryRequestDTO requestDTO
    ) {

        FundingTradeHistoryResponseDTO response = tradeHistoryService.getDailyTradeHistory(fundingId, requestDTO);

        return ResponseEntity.ok(ApiCommonResponse.createSuccess(response));
    }
}
