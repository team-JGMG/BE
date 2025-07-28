package org.bobj.orderbook.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.order.dto.response.OrderResponseDTO;
import org.bobj.orderbook.dto.response.OrderBookResponseDTO;
import org.bobj.orderbook.service.OrderBookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order-books")
@RequiredArgsConstructor
@Log4j2
@Api(tags="호가 API")
public class OrderBookController {

    private final OrderBookService orderBookService;

    @GetMapping("/{fundingId}")
    @ApiOperation(value = "호가창 정보 조회", notes = "특정 펀딩 ID에 대한 현재 호가창 정보를 조회합니다.")
    public ResponseEntity<ApiCommonResponse<OrderBookResponseDTO>> getOrderBook(
            @ApiParam(value = "펀딩 ID", example = "1", required = true)
            @PathVariable Long fundingId) {
        OrderBookResponseDTO orderBookResponse = orderBookService.getOrderBookByFundingId(fundingId);

        return ResponseEntity.ok(ApiCommonResponse.createSuccess(orderBookResponse));
    }
}
