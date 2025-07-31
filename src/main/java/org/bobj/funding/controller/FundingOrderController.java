package org.bobj.funding.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.dto.CustomSlice;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.funding.dto.FundingOrderLimitDTO;
import org.bobj.funding.dto.FundingOrderUserResponseDTO;
import org.bobj.funding.service.FundingOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/funding-order")
@RequiredArgsConstructor
@Log4j2
@Api(tags="펀딩 주문 API")
public class FundingOrderController {
    private final FundingOrderService fundingOrderService;

    @PostMapping
    @ApiOperation(value = "펀딩 주문 생성", notes = "펀딩 ID, 회원 ID, 구매 주식 수 정보를 통해 펀딩 주문을 생성합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "사용자 ID", required = true, dataType = "long", paramType = "query"),
            @ApiImplicitParam(name = "fundingId", value = "펀딩 ID", required = true, dataType = "long", paramType = "query"),
            @ApiImplicitParam(name = "shareCount", value = "구매 주식 수", required = true, dataType = "int", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "펀딩 주문 생성 성공"),
            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<Void>> createFundingOrder(
            @RequestParam Long userId,
            @RequestParam Long fundingId,
            @RequestParam int shareCount) {
        fundingOrderService.createFundingOrder(userId, fundingId, shareCount);
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

    @GetMapping("/{userId}")
    @ApiOperation(value = "사용자의 투자 주문 목록 조회", notes = "주문 ID, Status에 따른 투자 주문 목록을 조회합니다. (무한스크롤 구현)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "사용자 ID", required = true, dataType = "long", paramType = "path"),
            @ApiImplicitParam(name = "status", value = "주문 상태(pending -> 대기중, refunded -> 기간 만료(펀딩 실패)", defaultValue = "pending" ,dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "page", value = "페이지 번호 (0부터 시작)", defaultValue = "0" ,dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "size", value = "한 페이지당 항목 수", defaultValue = "10", dataType = "int", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "사용자의 투자 주문 목록 조회 성공", response = FundingOrderUserResponseDTO.class, responseContainer = "CustomSlice"),
            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<CustomSlice<FundingOrderUserResponseDTO>>> getFundingOrderUsers(
            @PathVariable @ApiParam(value = "사용자 ID", required = true) Long userId,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        CustomSlice<FundingOrderUserResponseDTO> response = fundingOrderService.getFundingOrderUsers(userId, status, page, size);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(response));
    }

    @GetMapping("/limit")
    @ApiOperation(value = "사용자의 펀딩 주문 가능 정보 조회", notes = "주문 ID, 펀딩 ID에 따른 가능한 펀딩 주문 정보를 조회합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "사용자 ID",required = true, dataType = "long", paramType = "query"),
            @ApiImplicitParam(name = "fundingId", value = "펀딩 ID",required = true, dataType = "long", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "사용자의 펀딩 주문 가능 정보 조회 성공", response = FundingOrderLimitDTO.class),
            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<FundingOrderLimitDTO>> getFundingOrderLimit(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "1") Long fundingId
    ){
        FundingOrderLimitDTO response = fundingOrderService.getFundingOrderLimit(userId, fundingId);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(response));
    }
}
