package org.bobj.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TokenDTO {
    private String accessToken;
    private String refreshToken;
    private Boolean isAdmin;    // 편의를 위해 유지
    private String role;        // 추가

    public TokenDTO(String accessToken, String refreshToken, boolean isAdmin) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.isAdmin = isAdmin;
        this.role = isAdmin ? "ADMIN" : "USER";  // role 자동 설정
    }
}