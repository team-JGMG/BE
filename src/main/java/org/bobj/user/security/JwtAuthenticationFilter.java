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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final CookieUtil cookieUtil;

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
        // 특정 경로를 허용하거나 "/api/"로 시작하는 경로 모두 허용
        return "/api/auth/login/callback".equals(path) ||
                path.startsWith("/api/") ||  // 와일드카드 대신 startsWith 사용
                "/api/auth/signup".equals(path);
    }



    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        log.debug("JWT Filter 처리: {} {}", method, path);

        String token = jwtTokenProvider.resolveToken(request);

        // Swagger 요청은 JWT 인증 건너뜀
        if (path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/webjars/") ||
                path.contains("swagger") ||
                path.endsWith("favicon.ico")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ================================
        // 🚀 개발용 설정: 토큰이 없으면 그냥 통과
        // ================================
        if (token == null) {
            log.debug("개발 모드: 토큰 없음 - 인증 없이 통과: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // ================================
        // 🚀 개발용 설정: 토큰이 있으면 처리 시도, 실패해도 통과
        // ================================
        if (jwtTokenProvider.validateToken(token)) {
            // 1. 토큰이 유효한 경우

            // 1-1. 사전 갱신 체크 (Access Token만 대상)
            String tokenType = jwtTokenProvider.getClaims(token).get("type", String.class);
            if ("access".equals(tokenType) && jwtTokenProvider.shouldPreemptivelyRefresh(token)) {
                log.info("토큰 만료 임박 - 사전 갱신 시도: {} ({}분 후 만료)",
                        path, jwtTokenProvider.getTokenRemainingMinutes(token));

                String preRefreshedToken = attemptPreemptiveTokenRefresh(token, request, response);
                if (preRefreshedToken != null) {
                    // 사전 갱신 성공 - 새 토큰으로 처리
                    log.info("사전 갱신 성공 - 새 토큰으로 인증 처리");
                    token = preRefreshedToken;
                } else {
                    // 사전 갱신 실패 - 기존 토큰으로 계속 진행 (아직 유효하므로 문제없음)
                    log.warn("사전 갱신 실패 - 기존 토큰으로 계속 진행");
                }
            }

            // 1-2. 토큰 인증 처리 (기존 토큰 또는 새 토큰)
            if (handleValidToken(token, path, response)) {
                log.debug("개발 모드: 유효한 토큰으로 인증 성공: {}", path);
                filterChain.doFilter(request, response);
            } else {
                log.warn("개발 모드: 토큰 처리 실패했지만 통과시킴: {}", path);
//                filterChain.doFilter(request, response);  // 🚀 개발용: 실패해도 통과
            }
            return;

        } else if (jwtTokenProvider.isTokenExpired(token)) {
            // 2. 토큰이 만료된 경우 - 기존 갱신 로직
            log.info("토큰 만료 감지 - 강제 갱신 시도: {}", path);
            String refreshedToken = attemptTokenRefresh(token, request, response);
            if (refreshedToken != null) {
                if (handleValidToken(refreshedToken, path, response)) {
                    log.debug("개발 모드: 토큰 갱신 후 인증 성공: {}", path);
                    filterChain.doFilter(request, response);
                } else {
                    log.warn("개발 모드: 갱신된 토큰 처리 실패했지만 통과시킴: {}", path);
                    filterChain.doFilter(request, response);  // 🚀 개발용: 실패해도 통과
                }
                return;
            } else {
                log.warn("개발 모드: 토큰 자동 갱신 실패했지만 통과시킴: {}", path);
                filterChain.doFilter(request, response);  // 🚀 개발용: 갱신 실패해도 통과
                return;
            }
        } else {
            // 3. 유효하지 않은 토큰
            log.warn("개발 모드: 유효하지 않은 토큰이지만 통과시킴: {}", path);
            filterChain.doFilter(request, response);  // 🚀 개발용: 무효한 토큰이어도 통과
            return;
        }

    }
        /* ================================
         * 🔒 운영용 JWT 필터 로직 (주석 처리)
         * 배포 시 위의 개발용 로직을 주석처리하고 아래 로직을 활성화하세요
         * ================================

        if (token == null) {
            // 토큰이 없는 경우
            if (path.startsWith("/api/auth/")) {
                // /api/auth/** 경로는 토큰 필수
                log.warn("인증 필요한 경로에 토큰 없음: {}", path);
                sendUnauthorizedResponse(response, "Access token required for this endpoint");
                return;
            } else if ("/api/signup".equals(path)) {
                // /api/signup 경로는 임시토큰 필수
                log.warn("회원가입 경로에 토큰 없음: {}", path);
                sendUnauthorizedResponse(response, "Pre-auth token required for signup");
                return;
            }
            // 기타 경로는 토큰 없어도 통과
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtTokenProvider.validateToken(token)) {
            // 1. 토큰이 유효한 경우
            String tokenType = jwtTokenProvider.getClaims(token).get("type", String.class);
            
            // 인증이 필요한 경로에서만 토큰 타입 검증
            if (path.startsWith("/api/auth/")) {
                // /api/auth/** 경로는 정규토큰(access)만 허용
                if (!"access".equals(tokenType)) {
                    log.warn("인증 필요한 경로에 부적절한 토큰 타입: {} (경로: {})", tokenType, path);
                    sendUnauthorizedResponse(response, "Access token required for authenticated endpoints");
                    return;
                }
            } else if ("/api/signup".equals(path)) {
                // /api/signup 경로는 임시토큰(pre-auth)만 허용
                if (!"pre-auth".equals(tokenType)) {
                    log.warn("회원가입 경로에 부적절한 토큰 타입: {} (경로: {})", tokenType, path);
                    sendUnauthorizedResponse(response, "Pre-auth token required for signup");
                    return;
                }
            }
            // 그외 /api/** 경로는 어떤 토큰이든 허용 (토큰 없이 접근 가능한 곳이므로)

            // 1-1. 사전 갱신 체크 (Access Token만 대상)
            if ("access".equals(tokenType) && jwtTokenProvider.shouldPreemptivelyRefresh(token)) {
                log.info("토큰 만료 임박 - 사전 갱신 시도: {} ({}분 후 만료)",
                    path, jwtTokenProvider.getTokenRemainingMinutes(token));

                String preRefreshedToken = attemptPreemptiveTokenRefresh(token, request, response);
                if (preRefreshedToken != null) {
                    // 사전 갱신 성공 - 새 토큰으로 처리
                    log.info("사전 갱신 성공 - 새 토큰으로 인증 처리");
                    token = preRefreshedToken;
                } else {
                    // 사전 갱신 실패 - 기존 토큰으로 계속 진행 (아직 유효하므로 문제없음)
                    log.warn("사전 갱신 실패 - 기존 토큰으로 계속 진행");
                }
            }

            // 1-2. 토큰 인증 처리 (기존 토큰 또는 새 토큰)
            if (handleValidToken(token, path, response)) {
                filterChain.doFilter(request, response);
            }
            return;

        } else if (jwtTokenProvider.isTokenExpired(token)) {
            // 2. 토큰이 만료된 경우
            String tokenType;
            try {
                tokenType = jwtTokenProvider.getClaims(token).get("type", String.class);
            } catch (Exception e) {
                // 토큰이 너무 손상되어 타입도 읽을 수 없는 경우
                if (path.startsWith("/api/auth/") || "/api/signup".equals(path)) {
                    log.warn("손상된 토큰으로 인증 필요한 경로 접근 시도: {}", path);
                    sendUnauthorizedResponse(response, "Invalid token format");
                    return;
                } else {
                    // 인증 불필요한 경로는 손상된 토큰 무시하고 통과
                    log.debug("손상된 토큰이지만 인증 불필요한 경로이므로 통과: {}", path);
                    filterChain.doFilter(request, response);
                    return;
                }
            }
            
            // 인증이 필요한 경로에서만 만료 토큰 처리
            if (path.startsWith("/api/auth/") || "/api/signup".equals(path)) {
                // pre-auth 토큰은 갱신하지 않음 (임시 토큰이므로)
                if ("pre-auth".equals(tokenType)) {
                    log.warn("만료된 임시토큰으로 접근 시도: {}", path);
                    sendUnauthorizedResponse(response, "Pre-auth token expired, please re-authenticate");
                    return;
                }
                
                // access 토큰만 자동 갱신 시도
                if ("access".equals(tokenType)) {
                    log.info("토큰 만료 감지 - 자동 갱신 시도: {}", path);
                    String refreshedToken = attemptTokenRefresh(token, request, response);
                    if (refreshedToken != null) {
                        if (handleValidToken(refreshedToken, path, response)) {
                            filterChain.doFilter(request, response);
                        }
                        return;
                    } else {
                        log.warn("토큰 자동 갱신 실패: {}", path);
                        sendUnauthorizedResponse(response, "Token refresh failed, please re-authenticate");
                        return;
                    }
                } else {
                    log.warn("알 수 없는 만료된 토큰 타입: {} (경로: {})", tokenType, path);
                    sendUnauthorizedResponse(response, "Unknown expired token type");
                    return;
                }
            } else {
                // 인증 불필요한 경로는 만료된 토큰 무시하고 통과
                log.debug("만료된 토큰이지만 인증 불필요한 경로이므로 통과: {}", path);
                filterChain.doFilter(request, response);
                return;
            }
        } else {
            // 3. 유효하지 않은 토큰
            log.warn("유효하지 않은 토큰으로 접근 시도: {}", path);
            
            // 인증이 필요한 경로인지 확인
            if (path.startsWith("/api/auth/") || "/api/signup".equals(path)) {
                sendUnauthorizedResponse(response, "Invalid token");
                return;
            } else {
                // 인증이 필요하지 않은 경로는 무효한 토큰 무시하고 통과
                log.debug("인증 불필요한 경로에서 무효한 토큰 무시: {}", path);
                filterChain.doFilter(request, response);
                return;
            }
        }
        */


    /**
     * 자동 토큰 갱신 시도 (만료 후 강제 갱신)
     */
    private String attemptTokenRefresh(String expiredToken, HttpServletRequest request, HttpServletResponse response) {
        try {
            log.info("Access Token 자동 갱신 시도 (만료 후)");

            // 1. 만료된 토큰에서 이메일 추출
            String email = jwtTokenProvider.getUserEmailFromExpiredToken(expiredToken);
            if (email == null) {
                log.warn("만료된 토큰에서 이메일 추출 실패");
                return null;
            }

            // 2. 사용자 정보 조회 (예외 처리 추가)
            UserVO user;
            try {
                user = userService.findUserVOByEmail(email);
            } catch (UsernameNotFoundException e) {
                log.warn("강제 갱신: 사용자를 찾을 수 없음: {} (테스트 환경일 가능성)", email);
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
                cookieUtil.deleteAccessTokenCookie(response, request);
                return null;
            }

            // 5. 새로운 Access Token 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(email, user.getUserId(), user.isAdmin());

            // 6. 쿠키에 새 Access Token 설정
            cookieUtil.setAccessTokenCookie(response, request, newAccessToken);

            log.info("Access Token 자동 갱신 성공 (만료 후): {}", email);
            return newAccessToken;

        } catch (Exception e) {
            log.error("토큰 자동 갱신 중 예외 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 사전 토큰 갱신 시도 (만료 전)
     * 실패해도 기존 토큰이 유효하므로 서비스 중단 없음
     */
    private String attemptPreemptiveTokenRefresh(String validToken, HttpServletRequest request, HttpServletResponse response) {
        try {
            log.info("Access Token 사전 갱신 시도 (만료 {}분 전)",
                jwtTokenProvider.getTokenRemainingMinutes(validToken));

            // 1. 유효한 토큰에서 사용자 정보 추출
            String email = jwtTokenProvider.getUserPk(validToken);
            Long userId = jwtTokenProvider.getUserId(validToken);

            if (email == null || userId == null) {
                log.warn("사전 갱신: 토큰에서 사용자 정보 추출 실패");
                return null;
            }

            // 2. 사용자 정보 조회 (예외 처리 추가)
            UserVO user;
            try {
                user = userService.findUserVOByEmail(email);
            } catch (UsernameNotFoundException e) {
                log.warn("사전 갱신: 사용자를 찾을 수 없음: {} (테스트 환경일 가능성)", email);
                return null;
            }

            // 3. 소셜 로그인 정보에서 Refresh Token 조회
            SocialLoginsVO socialLogin = userService.findSocialLoginByUserId(user.getUserId());
            if (socialLogin == null || socialLogin.getRefreshToken() == null) {
                log.warn("사전 갱신: Refresh Token을 찾을 수 없음: {}", email);
                return null;
            }

            // 4. Refresh Token 유효성 검증
            if (!jwtTokenProvider.validateToken(socialLogin.getRefreshToken())) {
                log.warn("사전 갱신: Refresh Token이 만료됨: {}", email);
                // 사전 갱신에서는 강제 로그아웃 하지 않음 (기존 토큰이 아직 유효)
                return null;
            }

            // 5. 새로운 Access Token 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(email, user.getUserId(), user.isAdmin());

            // 6. 쿠키에 새 Access Token 설정
            cookieUtil.setAccessTokenCookie(response, request, newAccessToken);

            // 7. 사전 갱신 표시 헤더 추가 (선택사항)
            response.setHeader("X-Token-Preemptively-Refreshed", "true");
            response.setHeader("X-Token-Remaining-Minutes", String.valueOf(jwtTokenProvider.getTokenRemainingMinutes(validToken)));

            log.info("Access Token 사전 갱신 성공: {} (기존 토큰 {}분 남음)",
                email, jwtTokenProvider.getTokenRemainingMinutes(validToken));
            return newAccessToken;

        } catch (Exception e) {
            log.warn("사전 토큰 갱신 중 예외 발생 (기존 토큰으로 계속 진행): {}", e.getMessage());
            // 사전 갱신 실패는 심각한 문제가 아님 - 기존 토큰으로 계속 진행
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