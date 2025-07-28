package org.bobj.user.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "인증 관련 응답 DTO")
public class AuthResponseDTO {

    @ApiModelProperty(value = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...", required = true)
    private String accessToken;

    @ApiModelProperty(value = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9...", required = false)
    private String refreshToken;

    @ApiModelProperty(value = "사용자 ID", example = "12345", required = true)
    private Long userId;

    @ApiModelProperty(value = "관리자 여부", example = "false", required = true)
    private Boolean isAdmin;

    @ApiModelProperty(value = "사용자 역할", example = "USER", allowableValues = "USER, ADMIN", required = true)
    private String role;

    @ApiModelProperty(value = "토큰 타입", example = "Bearer", required = false)
    private String tokenType;

    @ApiModelProperty(value = "액세스 토큰 만료시간 (초)", example = "1800", required = false)
    private Long expiresIn;

    @ApiModelProperty(value = "토큰 발급 시각", example = "2025-07-27T15:30:00", required = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedAt;

    // 기본 생성자들 (기존 호환성)
    public AuthResponseDTO(String accessToken, String refreshToken, boolean isAdmin) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.isAdmin = isAdmin;
        this.role = isAdmin ? "ADMIN" : "USER";
        this.tokenType = "Bearer";
        this.expiresIn = 1800L; // 30분
        this.issuedAt = LocalDateTime.now();
    }
//
    public AuthResponseDTO(String accessToken, String refreshToken, Long userId, boolean isAdmin) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.isAdmin = isAdmin;
        this.role = isAdmin ? "ADMIN" : "USER";
        this.tokenType = "Bearer";
        this.expiresIn = 1800L; // 30분
        this.issuedAt = LocalDateTime.now();
    }
//
//    /**
//     * 성공적인 로그인 응답 생성
//     */
//    public static AuthResponseDTO loginSuccess(String accessToken, String refreshToken, Long userId, boolean isAdmin) {
//        return AuthResponseDTO.builder()
//                .accessToken(accessToken)
//                .refreshToken(refreshToken)
//                .userId(userId)
//                .isAdmin(isAdmin)
//                .role(isAdmin ? "ADMIN" : "USER")
//                .tokenType("Bearer")
//                .expiresIn(1800L)
//                .issuedAt(LocalDateTime.now())
//                .build();
//    }
//
//    /**
//     * 토큰 갱신 응답 생성
//     */
//    public static AuthResponseDTO tokenRefresh(String accessToken, String refreshToken, Long userId, boolean isAdmin) {
//        return AuthResponseDTO.builder()
//                .accessToken(accessToken)
//                .refreshToken(refreshToken)
//                .userId(userId)
//                .isAdmin(isAdmin)
//                .role(isAdmin ? "ADMIN" : "USER")
//                .tokenType("Bearer")
//                .expiresIn(1800L)
//                .issuedAt(LocalDateTime.now())
//                .build();
//    }
//
//    /**
//     * 액세스 토큰만 갱신하는 응답 생성
//     */
//    public static AuthResponseDTO accessTokenOnly(String accessToken, Long userId, boolean isAdmin) {
//        return AuthResponseDTO.builder()
//                .accessToken(accessToken)
//                .userId(userId)
//                .isAdmin(isAdmin)
//                .role(isAdmin ? "ADMIN" : "USER")
//                .tokenType("Bearer")
//                .expiresIn(1800L)
//                .issuedAt(LocalDateTime.now())
//                .build();
//    }
//
//    /**
//     * 민감정보 마스킹 (로깅용)
//     */
//    public String toMaskedString() {
//        return String.format("AuthResponse{userId=%d, role='%s', issuedAt=%s}",
//                userId, role, issuedAt);
//    }
}
