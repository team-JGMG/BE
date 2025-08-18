package org.bobj.orderbook.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.orderbook.dto.response.OrderBookResponseDTO;
import org.bobj.orderbook.service.OrderBookService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;

@Log4j2
@Controller
@RequiredArgsConstructor
@Api(tags = "호가창 실시간 WebSocket API")
public class OrderBookWebSocketController {

    private final OrderBookService orderBookService;

    @MessageMapping("/order-book/{fundingId}")
    @SendTo("/topic/order-book/{fundingId}")
    @ApiOperation(value = "호가창 실시간 구독", notes = "STOMP 메시지를 통해 해당 펀딩의 호가창을 실시간으로 받아옵니다.")
    public OrderBookResponseDTO sendMessage(@DestinationVariable Long fundingId){

        OrderBookResponseDTO orderBook = orderBookService.getOrderBookByFundingId(fundingId);
        log.info("OrderBookWebSocketController - sendMessage 호출됨!");
        log.info("수신된 fundingId: {}", fundingId);
        log.info("수신된 메시지: {}", orderBook);

        return orderBook;
    }
}
