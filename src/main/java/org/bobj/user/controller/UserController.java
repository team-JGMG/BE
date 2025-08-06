package org.bobj.user.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.user.dto.response.UserResponseDTO;
import org.bobj.user.security.UserPrincipal;
import org.bobj.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Api(tags = "사용자 정보 API")
public class UserController {
    private final UserService userService;

    /**
     * 내 정보 조회 (전체 개인정보 포함)
     */
    @GetMapping("/me")
    @ApiOperation(value = "내 정보 조회", notes = "현재 로그인한 사용자의 모든 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "내 정보 조회 성공", response = UserResponseDTO.class),
            @ApiResponse(code = 401, message = "인증 필요\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 401,\n" +
                    "  \"code\": \"USER001\",\n" +
                    "  \"message\": \"인증이 필요합니다. 로그인 후 다시 시도해주세요.\",\n" +
                    "  \"path\": \"/api/users/me\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 500,\n" +
                    "  \"code\": \"USER002\",\n" +
                    "  \"message\": \"사용자 정보 조회 중 서버 오류가 발생했습니다.\",\n" +
                    "  \"path\": \"/api/users/me\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class)
    })
    public ResponseEntity<UserResponseDTO> getMyInfo(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        // UserPrincipal에서 userId를 가져와 DB에서 전체 사용자 정보 조회
        UserResponseDTO myInfo = userService.findUserInfoById(userPrincipal.getUserId());
        return ResponseEntity.ok(myInfo);
    }

    /**
     * 특정 사용자 정보 조회 (관리자 전용)
     */
    @GetMapping("/{userId}")
    @ApiOperation(value = "특정 사용자 정보 조회", notes = "관리자가 특정 사용자의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "사용자 정보 조회 성공", response = UserResponseDTO.class),
            @ApiResponse(code = 401, message = "인증 필요\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 401,\n" +
                    "  \"code\": \"USER003\",\n" +
                    "  \"message\": \"인증이 필요합니다. 로그인 후 다시 시도해주세요.\",\n" +
                    "  \"path\": \"/api/users/{userId}\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 403, message = "관리자 권한 필요\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 403,\n" +
                    "  \"code\": \"USER004\",\n" +
                    "  \"message\": \"관리자 권한이 필요합니다.\",\n" +
                    "  \"path\": \"/api/users/{userId}\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "사용자를 찾을 수 없음\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 404,\n" +
                    "  \"code\": \"USER005\",\n" +
                    "  \"message\": \"해당 사용자를 찾을 수 없습니다.\",\n" +
                    "  \"path\": \"/api/users/{userId}\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 500,\n" +
                    "  \"code\": \"USER006\",\n" +
                    "  \"message\": \"사용자 정보 조회 중 서버 오류가 발생했습니다.\",\n" +
                    "  \"path\": \"/api/users/{userId}\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class)
    })
    public ResponseEntity<UserResponseDTO> getUserInfo(@PathVariable @ApiParam(value = "조회할 사용자 ID", required = true, example = "12345") Long userId) {
        UserResponseDTO userInfo = userService.findUserInfoById(userId);
        return ResponseEntity.ok(userInfo);
    }
}