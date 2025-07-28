package org.bobj.user.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.user.util.CookieUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${custom.oauth2.redirect-uri}")
    private String frontendRedirectUri;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {  // ✅ ServletException 제거

        log.info("OAuth2 Login 성공. '사전 인증' 임시 JWT 발급을 시작합니다.");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 안전한 형 변환
        Object attributesObj = oAuth2User.getAttributes();
        if (!(attributesObj instanceof Map)) {
            throw new OAuth2AuthenticationException("Invalid OAuth2 user attributes format");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = (Map<String, Object>) attributesObj;

        // 카카오 계정 정보 안전한 추출
        String email = null;
        String nickname = null;

        // kakao_account에서 이메일 추출
        Object kakaoAccountObj = attributes.get("kakao_account");
        if (kakaoAccountObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) kakaoAccountObj;
            email = (String) kakaoAccount.get("email");

            // profile에서 닉네임 추출
            Object profileObj = kakaoAccount.get("profile");
            if (profileObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> profile = (Map<String, Object>) profileObj;
                nickname = (String) profile.get("nickname");
            }
        }

        String providerId = String.valueOf(attributes.get("id"));
        String provider = "kakao";  // 고정값

        log.info("임시 JWT 생성을 위한 정보: email={}, nickname={}, provider={}, providerId={}",
                email, nickname, provider, providerId);

        // DB 조회 없이, 소셜 정보만으로 '사전 인증' 상태의 임시 JWT를 생성합니다.
        String preAuthToken = jwtTokenProvider.createPreAuthToken(email, nickname, provider, providerId);

        log.info("사전 인증 토큰 생성 완료.");

        // 쿠키로 pre-auth 토큰 설정 (보안 강화)
        CookieUtil.setPreAuthTokenCookie(response, preAuthToken);

        // 프론트엔드로 상태만 전달 (토큰은 쿠키에 있음)
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("status", "PRE_AUTH")
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}