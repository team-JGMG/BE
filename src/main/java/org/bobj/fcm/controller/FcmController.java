package org.bobj.fcm.controller;

import lombok.RequiredArgsConstructor;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.fcm.dto.request.FcmRequestDto;
import org.bobj.fcm.service.FcmService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fcm")
public class FcmController {
    private final FcmService fcmService;

    // 1. client가 server로 알림 생성 요청
    @PostMapping("/pushMessage")
    public ApiCommonResponse<String> pushMessage(@RequestBody FcmRequestDto requestDTO) throws IOException {
        System.out.println(requestDTO.getDeviceToken() + " "
                +requestDTO.getTitle() + " " + requestDTO.getBody());
        fcmService.sendMessageTo(requestDTO);
        return ApiCommonResponse.createSuccess("fcm alarm success");
    }
}
