package org.bobj.device.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.device.dto.request.UserDeviceTokenRequestDTO;
import org.bobj.device.service.UserDeviceTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Log4j2
@Api(tags = "디바이스 토큰 API")
public class UserDeviceTokenController {

    private final UserDeviceTokenService userDeviceTokenService;

    @PostMapping("/users/{userId}/device-token")
    @ApiOperation(value = "디바이스 토큰 등록/갱신", notes = "사용자의 푸시 알림을 위한 디바이스 토큰을 등록하거나 갱신합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "디바이스 토큰을 등록할 사용자 ID", required = true, dataType = "long", paramType = "path")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "디바이스 토큰 등록 성공", response = ApiCommonResponse.class),
            @ApiResponse(code = 400, message = "잘못된 요청 (예: 토큰 값 누락)", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "사용자를 찾을 수 없음", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<String>> registerDeviceToken(
            @PathVariable @ApiParam(value = "사용자 ID", required = true) Long userId,
            @RequestBody @ApiParam(value = "디바이스 토큰 DTO", required = true) UserDeviceTokenRequestDTO requestDTO
    ) {
        log.debug("디바이스 토큰 등록 요청 - userId: {}, token: {}", userId, requestDTO.getDeviceToken());

        // 서비스 레이어에서 토큰 등록/갱신 로직 처리
        userDeviceTokenService.registerToken(userId, requestDTO.getDeviceToken());

        return ResponseEntity.ok(ApiCommonResponse.createSuccess("디바이스 토큰이 성공적으로 등록/갱신되었습니다."));
    }
}
