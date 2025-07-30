package org.bobj.user.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.user.domain.SocialLoginsVO;
import org.bobj.user.dto.response.AuthResponseDTO;
import org.bobj.user.service.UserService;
import org.bobj.user.util.CookieUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
// @Component 제거 - SecurityConfig에서 Bean으로 등록
// @RequiredArgsConstructor 제거 - 수동 생성자 사용
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Value("${custom.oauth2.redirect-uri}")
    private String frontendRedirectUri;

    // 수동 생성자 추가
    public OAuth2LoginSuccessHandler(JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        log.info("OAuth2 Login 성공. 사용자 존재 여부 확인 및 토큰 발급을 시작합니다.");

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

        log.info("소셜 로그인 정보: email={}, nickname={}, provider={}, providerId={}", email, nickname, provider, providerId);

        try {
            // 1. 사용자 존재 여부 확인
            Optional<SocialLoginsVO> existingSocialLogin = userService.findSocialLoginByProviderAndProviderId(provider, providerId);
            
            if (existingSocialLogin.isPresent()) {
                // 2-1. 기존 회원 - 바로 액세스 토큰 발급하고 메인페이지로 리다이렉트
                log.info("기존 회원 감지. 액세스 토큰 발급 후 메인페이지로 리다이렉트");
                
                AuthResponseDTO authResponse = userService.loginExistingSocialUser(provider, providerId);
                
                // 액세스 토큰을 쿠키로 설정
                CookieUtil.setAccessTokenCookie(response, authResponse.getAccessToken());
                
                // 메인페이지로 리다이렉트 (성공 상태)
                String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                        .queryParam("status", "SUCCESS")
                        .build().toUriString();
                
                log.info("기존 회원 로그인 완료. 메인페이지로 리다이렉트: {}", targetUrl);
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                
            } else {
                // 2-2. 신규 회원 - pre-auth 토큰 발급하고 회원가입 페이지로 리다이렉트
                log.info("신규 회원 감지. pre-auth 토큰 발급 후 회원가입 페이지로 리다이렉트");
                
                // pre-auth 토큰 생성
                String preAuthToken = jwtTokenProvider.createPreAuthToken(email, nickname, provider, providerId);
                
                // 쿠키로 pre-auth 토큰 설정
                CookieUtil.setPreAuthTokenCookie(response, preAuthToken);
                
                // 회원가입 페이지로 리다이렉트 (회원가입 필요 상태)
                String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                        .queryParam("status", "SIGNUP_REQUIRED")
                        .queryParam("email", email)
                        .build().toUriString();
                
                log.info("신규 회원 감지. 회원가입 페이지로 리다이렉트: {}", targetUrl);
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
            }
            
        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생: email={}, provider={}, providerId={}", email, provider, providerId, e);
            
            // 오류 발생 시 에러 페이지로 리다이렉트
            String errorUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                    .queryParam("status", "ERROR")
                    .queryParam("message", "로그인 처리 중 오류가 발생했습니다.")
                    .build().toUriString();
            
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }
}