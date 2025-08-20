package org.bobj.order.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.constants.ErrorCode;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.order.dto.request.OrderRequestDTO;
import org.bobj.order.dto.response.OrderResponseDTO;
import org.bobj.order.service.OrderService;
import org.bobj.orderbook.dto.response.OrderBookResponseDTO;
import org.bobj.orderbook.service.OrderBookService;
import org.bobj.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/auth/orders")
@RequiredArgsConstructor
@Log4j2
@Api(tags = "거래 주문 API")
public class OrderController {

    private final SimpMessagingTemplate messagingTemplate;

    private final OrderService service;
    private final OrderBookService orderBookService;

    @PostMapping("")
    @ApiOperation(value = "거래 주문 등록", notes = "새로운 거래 주문 정보를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "거래 주문 성공", response = ApiCommonResponse.class),
            @ApiResponse(code = 400, message = "잘못된 요청 (펀딩 미종료, 주식 미보유, 수량 부족, 발행량 초과 등)\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 400,\n" +
                    "  \"code\": \"OB001\",\n" +
                    "  \"message\": \"펀딩이 종료된 후에만 거래가 가능합니다.\",\n" +
                    "  \"path\": \"/api/auth/orders\"\n" +
                    "}\n" +
                    "```\n\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 400,\n" +
                    "  \"code\": \"OB002\",\n" +
                    "  \"message\": \"해당 종목을 보유하고 있지 않습니다.\",\n" +
                    "  \"path\": \"/api/auth/orders\"\n" +
                    "}\n" +
                    "```\n\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 400,\n" +
                    "  \"code\": \"OB003\",\n" +
                    "  \"message\": \"보유 주식 수량보다 많은 수량을 매도할 수 없습니다.\",\n" +
                    "  \"path\": \"/api/auth/orders\"\n" +
                    "}\n" +
                    "```\n\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 400,\n" +
                    "  \"code\": \"OB004\",\n" +
                    "  \"message\": \"총 발행 주식 수를 초과하는 수량은 매수할 수 없습니다.\",\n" +
                    "  \"path\": \"/api/auth/orders\"\n" +
                    "}\n" +
                    "```\n\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 400,\n" +
                    "  \"code\": \"C001\",\n" +
                    "  \"message\": \"잘못된 입력 값입니다.\",\n" +
                    "  \"path\": \"/api/auth/orders\"\n" +
                    "}\n" +
                    "```\n\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 400,\n" +
                    "  \"code\": \"OB006\",\n" +
                    "  \"message\": \"거래 가능 시간(09:00~15:00)이 아닙니다.\",\n" +
                    "  \"path\": \"/api/auth/orders\"\n" +
                    "}\n" +
                    "```",
                    response = ErrorResponse.class),
            @ApiResponse(code = 409, message = "충돌 (예: 이미 체결된 주문)\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 409,\n" +
                    "  \"code\": \"OB005\",\n" +
                    "  \"message\": \"이미 체결된 주문입니다.\",\n" +
                    "  \"path\": \"/api/auth/orders\"\n" +
                    "}\n" +
                    "```",
                    response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<OrderResponseDTO>> placeOrder(
            @ApiIgnore @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @ApiParam(value = "거래 주문 DTO", required = true) OrderRequestDTO dto) {

        Long userId = principal.getUserId();
        OrderResponseDTO created = service.placeOrder(userId, dto);

        ApiCommonResponse<OrderResponseDTO> response = ApiCommonResponse.createSuccess(created);

        // 1. 주문 완료 후, 해당 펀딩 ID의 캐시 삭제
        orderBookService.evictOrderBookCache(created.getFundingId());

        // 2. 소켓 메세지 pub
        publishOrderBookUpdate(created.getFundingId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("")
    @ApiOperation(value = "거래 주문 내역 조회", notes = "사용자의 거래 주문 내역을 조회합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderType", value = "주문 타입 (BUY, SELL)", required = false, dataType = "string", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "거래 주문 내역 조회 성공", response = OrderResponseDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "잘못된 요청 (예: 유효하지 않은 파라미터)", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "조회된 주문 내역 없음", response = ErrorResponse.class), // 404는 보통 조회 결과가 없을 때 사용. 200 OK에 빈 리스트 반환이 일반적
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<List<OrderResponseDTO>>> getOrderHistory(
            @ApiIgnore @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "orderType", required = false) String orderType
    ) {
        Long userId = principal.getUserId();

        List<OrderResponseDTO> orderHistory = service.getOrderHistoryByUserId(userId, orderType);

        return ResponseEntity.ok(ApiCommonResponse.createSuccess(orderHistory));
    }


    @PatchMapping("/{orderId}")
    @ApiOperation(value = "거래 주문 취소", notes = "주어진 주문 ID에 해당하는 거래 주문을 취소합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderId", value = "취소할 주문 ID", required = true, dataType = "long", paramType = "path")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "주문 취소 성공", response = ApiCommonResponse.class),
            @ApiResponse(code = 400, message = "잘못된 요청 (예: 이미 취소된 주문)", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "해당 주문을 찾을 수 없음", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<String>> cancelOrder(@PathVariable Long orderId) {
        Long fundingId = service.cancelOrder(orderId);

        // 1. 주문 취소 후, 해당 펀딩 ID의 캐시를 먼저 삭제
        orderBookService.evictOrderBookCache(fundingId);

        //소켓 메세지 pub
        publishOrderBookUpdate(fundingId);

        return ResponseEntity.ok(ApiCommonResponse.createSuccess("주문이 성공적으로 취소되었습니다."));
    }

    private void publishOrderBookUpdate(Long fundingId) {
        try {
            OrderBookResponseDTO orderBook = orderBookService.getOrderBookByFundingId(fundingId);
            String destination = "/topic/order-book/" + fundingId;

            messagingTemplate.convertAndSend(destination, orderBook);
            log.info("Order book update published to topic {}: {}", destination, orderBook);
        } catch (Exception e) {
            log.error("Failed to publish order book update for fundingId {}: {}", fundingId, e.getMessage(), e);
        }
    }
}
