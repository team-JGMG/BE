package org.bobj.user.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.user.domain.SocialLoginsVO;
import org.bobj.user.domain.UserVO;
import org.bobj.user.service.UserService;
import org.bobj.user.util.CookieUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        log.debug("JWT Filter 처리: {} {}", method, path);

        // 토큰 추출 (Authorization Header 또는 Cookie에서)
        String token = jwtTokenProvider.resolveToken(request);

        if (token == null) {
            // 토큰이 없는 경우는 Spring Security가 처리 (permitAll이면 통과, 아니면 401)
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 유효성 검사
        if (jwtTokenProvider.validateToken(token)) {
            // 정상 토큰 → 기존 로직
            handleValidToken(token, path, response);
        } else if (jwtTokenProvider.isTokenExpired(token)) {
            // 만료된 토큰 → 자동 갱신 시도
            String refreshedToken = attemptTokenRefresh(token, request, response);
            if (refreshedToken != null) {
                handleValidToken(refreshedToken, path, response);
            } else {
                // 갱신 실패 → 401 처리
                log.warn("토큰 자동 갱신 실패: {}", path);
                sendUnauthorizedResponse(response, "Token refresh failed");
                return;
            }
        } else {
            // 잘못된 토큰 → 401 처리
            log.warn("유효하지 않은 토큰: {}", path);
            sendUnauthorizedResponse(response, "Invalid token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 유효한 토큰 처리
     */
    private void handleValidToken(String token, String path, HttpServletResponse response) throws IOException {
        try {
            String tokenType = jwtTokenProvider.getClaims(token).get("type", String.class);

            if ("access".equals(tokenType)) {
                // Access Token - 일반적인 API 인증
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Access Token 인증 성공: {}", authentication.getName());

            } else if ("pre-auth".equals(tokenType)) {
                // Pre-Auth Token - 특정 경로에서만 허용
                if (isPreAuthAllowedPath(path)) {
                    log.debug("Pre-Auth Token 허용 경로: {}", path);
                    // 인증 컨텍스트는 설정하지 않고 통과
                } else {
                    log.warn("Pre-Auth Token 비허용 경로: {}", path);
                    sendUnauthorizedResponse(response, "Pre-auth token not allowed for this endpoint");
                    return;
                }
            } else {
                log.warn("알 수 없는 토큰 타입: {}", tokenType);
                sendUnauthorizedResponse(response, "Invalid token type");
                return;
            }

        } catch (Exception e) {
            log.error("토큰 처리 중 오류 발생", e);
            sendUnauthorizedResponse(response, "Token processing error");
            return;
        }
    }

    /**
     * 자동 토큰 갱신 시도
     */
    private String attemptTokenRefresh(String expiredToken, HttpServletRequest request, HttpServletResponse response) {
        try {
            log.info("Access Token 자동 갱신 시도");
            
            // 1. 만료된 토큰에서 이메일 추출
            String email = jwtTokenProvider.getUserEmailFromExpiredToken(expiredToken);
            if (email == null) {
                log.warn("만료된 토큰에서 이메일 추출 실패");
                return null;
            }
            
            // 2. 사용자 정보 조회
            UserVO user = userService.findUserVOByEmail(email);
            if (user == null) {
                log.warn("사용자를 찾을 수 없음: {}", email);
                return null;
            }
            
            // 3. 소셜 로그인 정보에서 Refresh Token 조회
            SocialLoginsVO socialLogin = userService.findSocialLoginByUserId(user.getUserId());
            if (socialLogin == null || socialLogin.getRefreshToken() == null) {
                log.warn("Refresh Token을 찾을 수 없음: {}", email);
                return null;
            }
            
            // 4. Refresh Token 유효성 검증
            if (!jwtTokenProvider.validateToken(socialLogin.getRefreshToken())) {
                log.warn("Refresh Token이 만료됨: {}", email);
                // Refresh Token도 만료된 경우 → 로그아웃 처리
                userService.removeRefreshToken(email);
                CookieUtil.deleteAccessTokenCookie(response);
                return null;
            }
            
            // 5. 새로운 Access Token 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(email, user.getIsAdmin());
            
            // 6. 쿠키에 새 Access Token 설정
            CookieUtil.setAccessTokenCookie(response, newAccessToken);
            
            log.info("Access Token 자동 갱신 성공: {}", email);
            return newAccessToken;
            
        } catch (Exception e) {
            log.error("토큰 자동 갱신 중 예외 발생", e);
            return null;
        }
    }

    /**
     * Pre-Auth 토큰이 허용되는 경로인지 확인
     */
    private boolean isPreAuthAllowedPath(String path) {
        return "/api/auth/oauth/callback".equals(path) ||
                "/api/auth/signup/complete".equals(path);
    }

    /**
     * 401 Unauthorized 응답 전송
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"error\":\"%s\"}", message));
    }
}