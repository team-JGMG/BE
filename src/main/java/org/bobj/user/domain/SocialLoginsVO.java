package org.bobj.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginsVO {

    private Long socialId;        // social_id BIGINT
    private Long userId;          // user_id BIGINT (FK)
    private String provider;      // provider VARCHAR(20) (e.g., "kakao", "naver")
    private String providerId;    // provider_id VARCHAR(255) (소셜 서비스의 고유 ID)
    private String refreshToken;  // refresh_token TEXT
    private Date tokenExpiresAt;  // token_expires_at DATETIME
    private String profileData;   // profile_data JSON (JSON 문자열 그대로 저장)
    private Date createdAt;       // created_at DATETIME

    @Builder
    public SocialLoginsVO(Long userId, String provider, String providerId, String refreshToken, String profileData) {
        this.userId = userId;
        this.provider = provider;
        this.providerId = providerId;
        this.refreshToken = refreshToken;
        this.profileData = profileData;
        this.createdAt = new Date();
    }
}