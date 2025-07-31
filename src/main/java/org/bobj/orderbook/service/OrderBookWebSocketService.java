package org.bobj.orderbook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.order.mapper.OrderMapper;
import org.bobj.orderbook.dto.response.OrderBookResponseDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class OrderBookWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    private final OrderBookService orderBookService;

    // 호가창 업데이트를 웹소켓으로 발행
    public void publishOrderBookUpdate(Long fundingId) {
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
