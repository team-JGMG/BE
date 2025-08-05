package org.bobj.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.orderbook.dto.response.OrderBookResponseDTO;
import org.bobj.orderbook.service.OrderBookService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "Swagger 테스트 API")
@Log4j2
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/test")
    @ApiOperation(value = "헬로 응답", notes = "단순한 연결 테스트용 API입니다.")
    public String hello() {
        return "Hello";
    }


    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/test/publish")
    public String publishTestMessage(@RequestParam(defaultValue = "1") Long fundingId) {
        String destination = "/topic/order-book/" + fundingId;
        String message = "Hello World";

        try {
            messagingTemplate.convertAndSend(destination, message);
            return "Published: " + message;
        } catch (Exception e) {
             return "Failed to publish message: " + e.getMessage();
        }
    }
}
