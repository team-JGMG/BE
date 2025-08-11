package org.bobj.notification.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.notification.dto.response.NotificationResponseDTO;
import org.bobj.notification.service.NotificationService;
import org.bobj.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/notifications")
@RequiredArgsConstructor
@Log4j2
@Api(tags="알림 API")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("")
    @ApiOperation(value = "알림 목록 조회", notes = "현재 로그인된 사용자의 알림 목록을 최신순으로 조회합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "readStatus", value = "읽음 상태로 필터링 (all, unread)", allowableValues = "all, unread", required = false, dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "page", value = "페이지 번호 (0부터 시작)", defaultValue = "0", required = false, dataType = "integer", paramType = "query"),
            @ApiImplicitParam(name = "size", value = "페이지당 항목 수", defaultValue = "20", required = false, dataType = "integer", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "알림 목록 조회 성공", response = NotificationResponseDTO.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "인증 실패 (로그인 필요)", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<List<NotificationResponseDTO>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "all") String readStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = principal.getUserId();

        List<NotificationResponseDTO> notifications = notificationService.getNotificationsByUserId(userId, readStatus, page, size);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(notifications));
    }

    @PutMapping("/{notificationId}/read")
    @ApiOperation(value = "특정 알림 읽음 처리", notes = "특정 알림을 '읽음' 상태로 변경합니다.")
    @ApiImplicitParam(name = "notificationId", value = "읽음 처리할 알림 ID", required = true, dataType = "long", paramType = "path")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "알림 읽음 처리 성공", response = ApiCommonResponse.class),
            @ApiResponse(code = 401, message = "인증 실패 (로그인 필요)", response = ErrorResponse.class),
            @ApiResponse(code = 403, message = "접근 권한 없음 (다른 사용자의 알림에 접근 시도)", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "해당 알림을 찾을 수 없음", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<String>> markNotificationAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @ApiParam(value = "알림 ID", required = true) Long notificationId) {
        Long userId = principal.getUserId();

        notificationService.markNotificationAsRead(userId, notificationId);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess("알림이 읽음 처리되었습니다."));
    }

    @PutMapping("/read-all")
    @ApiOperation(value = "모든 알림 읽음 처리", notes = "현재 로그인된 사용자의 모든 읽지 않은 알림을 '읽음' 상태로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "모든 알림 읽음 처리 성공", response = ApiCommonResponse.class),
            @ApiResponse(code = 401, message = "인증 실패 (로그인 필요)", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<String>> markAllNotificationsAsRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal.getUserId();
        notificationService.markAllNotificationsAsRead(userId);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess("모든 알림이 읽음 처리되었습니다."));
    }
}
