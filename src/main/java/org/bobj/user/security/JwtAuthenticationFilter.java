package org.bobj.user.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.user.domain.SocialLoginsVO;
import org.bobj.user.domain.UserVO;
import org.bobj.user.service.UserService;
import org.bobj.user.util.CookieUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    private boolean handleValidToken(String token, String path, HttpServletResponse response) throws IOException {
        try {
            String tokenType = jwtTokenProvider.getClaims(token).get("type", String.class);

            if ("access".equals(tokenType)) {
                // 액세스 토큰 처리 (UserPrincipal 사용)
                String email = jwtTokenProvider.getUserPk(token);
                Long userId = jwtTokenProvider.getClaims(token).get("userId", Long.class);
                String role = jwtTokenProvider.getClaims(token).get("role", String.class);

                // JWT 정보로 UserPrincipal 생성
                UserPrincipal userPrincipal = UserPrincipal.fromJwtClaims(userId, email, role);

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userPrincipal, null, userPrincipal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                return true;  //성공 시 true

            } else if ("pre-auth".equals(tokenType)) {
                // Pre-auth 토큰 처리
                if (isPreAuthAllowedPath(path)) {
                    String email = jwtTokenProvider.getUserPk(token);

                    // Pre-auth는 임시 인증이므로 최소 정보만
                    UserPrincipal tempPrincipal = UserPrincipal.fromJwtClaims(null, email, "USER");

                    // 권한 없는 임시 인증
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            tempPrincipal, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    return true;  // 성공 시 true
                } else {
                    log.warn("Pre-Auth Token 비허용 경로: {}", path);
                    sendUnauthorizedResponse(response, "Pre-auth token not allowed for this endpoint");
                    return false;  // 실패 시 false
                }
            } else {
                log.warn("알 수 없는 토큰 타입: {}", tokenType);
                sendUnauthorizedResponse(response, "Invalid token type");
                return false;  // 실패 시 false
            }

        } catch (Exception e) {
            log.error("토큰 처리 중 오류 발생", e);
            sendUnauthorizedResponse(response, "Token processing error");
            return false;  // 예외 시 false
        }
    }

    // Pre-auth 허용 경로 업데이트
    private boolean isPreAuthAllowedPath(String path) {
        return "/api/auth/login/callback".equals(path) ||
                "/api/auth/signup".equals(path);  // oauth/signup → signup으로 수정
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        log.debug("JWT Filter 처리: {} {}", method, path);

        String token = jwtTokenProvider.resolveToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtTokenProvider.validateToken(token)) {
            // 올바른 로직: 성공 시 true, 실패 시 false
            if (handleValidToken(token, path, response)) {
                // 성공 시 다음 필터로 진행
                filterChain.doFilter(request, response);
            }
            // 실패 시 handleValidToken에서 응답 처리했으므로 return
            return;

        } else if (jwtTokenProvider.isTokenExpired(token)) {
            String refreshedToken = attemptTokenRefresh(token, request, response);
            if (refreshedToken != null) {
                if (handleValidToken(refreshedToken, path, response)) {
                    filterChain.doFilter(request, response);
                }
                return;
            } else {
                log.warn("토큰 자동 갱신 실패: {}", path);
                sendUnauthorizedResponse(response, "Token refresh failed");
                return;
            }
        } else {
            log.warn("유효하지 않은 토큰: {}", path);
            sendUnauthorizedResponse(response, "Invalid token");
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
            String newAccessToken = jwtTokenProvider.createAccessToken(email, user.getUserId(), user.getIsAdmin());
            
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
     * 401 Unauthorized 응답 전송
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"error\":\"%s\"}", message));
    }
}