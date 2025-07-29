package org.bobj.funding.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.dto.FundingOrderRequestDTO;
import org.bobj.funding.service.FundingOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/funding/order")
@RequiredArgsConstructor
@Log4j2
@Api(tags="펀딩 주문 API")
public class FundingOrderController {
    private final FundingOrderService fundingOrderService;

    @PostMapping
    @ApiOperation(value = "펀딩 주문 생성", notes = "펀딩 ID, 회원 ID, 구매 주식 수 정보를 통해 펀딩 주문을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "펀딩 주문 생성 성공"),
            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<Void>> createFundingOrder(
            @RequestBody @ApiParam(value = "펀딩 주문 요청 DTO", required = true) FundingOrderRequestDTO requestDTO) {
        fundingOrderService.createFundingOrder(requestDTO.toVO());
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(null));
    }

    @PostMapping("/refund")
    @ApiOperation(value = "펀딩 주문 환불", notes = "주문 ID, 펀딩 ID, 주문 금액을 기반으로 환불을 처리합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderId", value = "주문 ID", required = true, dataType = "long", paramType = "query"),
            @ApiImplicitParam(name = "fundingId", value = "펀딩 ID", required = true, dataType = "long", paramType = "query"),
            @ApiImplicitParam(name = "orderPrice", value = "환불 금액", required = true, dataType = "BigDecimal", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "펀딩 주문 환불 성공"),
            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<Void>> refundFundingOrder(
            @RequestParam Long orderId,
            @RequestParam Long fundingId,
            @RequestParam BigDecimal orderPrice) {
        fundingOrderService.refundFundingOrder(orderId, fundingId, orderPrice);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(null));
    }
}
