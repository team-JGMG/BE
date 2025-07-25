package org.bobj.user.controller;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.user.domain.UserVO;
import org.bobj.user.dto.AdditionalInfoDTO;
import org.bobj.user.dto.TokenDTO;
import org.bobj.user.security.JwtTokenProvider;
import org.bobj.user.service.UserService;
import org.bobj.user.util.CookieUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/oauth/kakao-url")
    public ResponseEntity<Map<String, String>> getKakaoLoginUrl() {
        String kakaoLoginUrl = "http://localhost:8080/oauth2/authorization/kakao";
        return ResponseEntity.ok(Collections.singletonMap("url", kakaoLoginUrl));
    }

    @GetMapping("/oauth/callback")
    public ResponseEntity<?> checkUserExist(HttpServletRequest request, HttpServletResponse response) {
        log.info("=== OAuth Callback 시작 ===");
        
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("유효하지 않은 토큰으로 요청");
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Invalid token"));
        }

        Claims claims = jwtTokenProvider.getClaims(token);
        String email = claims.getSubject();
        String provider = claims.get("provider", String.class);
        String providerId = claims.get("providerId", String.class);
        
        log.info("토큰에서 추출한 정보 - email: {}, provider: {}, providerId: {}", email, provider, providerId);

        try {
            // 이메일로 사용자 존재 확인
            log.info("이메일로 사용자 조회 시작: {}", email);
            UserVO user = userService.findUserVOByEmail(email);
            log.info("사용자 조회 성공 - userId: {}, nickname: {}", user.getUserId(), user.getNickname());
            
            // 소셜 로그인 정보도 함께 확인하고 기존 사용자 로그인 처리
            log.info("소셜 로그인 처리 시작 - provider: {}, providerId: {}", provider, providerId);
            TokenDTO tokenDTO = userService.loginExistingSocialUser(provider, providerId);
            log.info("소셜 로그인 처리 성공 - 토큰 발급 완료");

            // Access Token을 쿠키로 설정
//            CookieUtil.setAccessTokenCookie(response, tokenDTO.getAccessToken());

            return ResponseEntity.ok(tokenDTO);
        } catch (UsernameNotFoundException e) {
            log.info("신규 사용자 감지 - email: {}", email);
            return ResponseEntity.ok(Map.of(
                    "exists", false,
                    "email", email
            ));
        } catch (Exception e) {
            log.error("OAuth Callback 처리 중 예외 발생 - email: {}, provider: {}, providerId: {}", 
                email, provider, providerId, e);
            throw e; // GlobalExceptionHandler가 처리하도록 다시 던짐
        }
    }

    @PostMapping("/signup/complete")
    public ResponseEntity<TokenDTO> registerFinal(@RequestBody AdditionalInfoDTO additionalInfoDTO,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        String preAuthToken = jwtTokenProvider.resolveToken(request);
        if (preAuthToken == null || !jwtTokenProvider.validateToken(preAuthToken)) {
            return ResponseEntity.status(401).body(null);
        }

        Claims claims = jwtTokenProvider.getClaims(preAuthToken);
        if (!"pre-auth".equals(claims.get("type", String.class))) {
            return ResponseEntity.status(403).body(null);
        }

        TokenDTO finalTokenDTO = userService.registerUserAndCreateFinalToken(additionalInfoDTO, claims);

        // Access Token을 쿠키로 설정
        CookieUtil.setAccessTokenCookie(response, finalTokenDTO.getAccessToken());

        return ResponseEntity.ok(finalTokenDTO);
    }

    /**
     * 로그아웃 - 쿠키 삭제 및 Refresh Token DB에서 제거
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = jwtTokenProvider.resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getUserPk(token);
            // Refresh Token DB에서 제거
            userService.removeRefreshToken(email);
        }

        // 쿠키 삭제
        CookieUtil.deleteAccessTokenCookie(response);

        return ResponseEntity.ok(Collections.singletonMap("message", "로그아웃 되었습니다."));
    }

    /**
     * Access Token 재발급
     * 클라이언트는 만료된 Access Token만 보내고, 서버에서 DB의 Refresh Token으로 갱신
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        log.info("토큰 갱신 요청 시작");
        
        // 쿠키에서 만료된 access token 추출
        String expiredAccessToken = jwtTokenProvider.resolveToken(request);
        
        if (expiredAccessToken == null) {
            log.warn("토큰이 제공되지 않음");
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "No token provided"));
        }
        
        // 만료된 토큰에서 사용자 정보 추출
        String email = jwtTokenProvider.getUserEmailFromExpiredToken(expiredAccessToken);
        if (email == null) {
            log.warn("토큰에서 이메일 추출 실패");
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Invalid token"));
        }
        
        log.info("토큰 갱신 대상 사용자: {}", email);
        
        try {
            // DB에서 해당 사용자의 refresh token으로 새 access token 발급
            TokenDTO newTokenDTO = userService.refreshAccessTokenByEmail(email);
            
            // 새로운 Access Token을 쿠키로 설정
            CookieUtil.setAccessTokenCookie(response, newTokenDTO.getAccessToken());
            
            log.info("토큰 갱신 완료: {}", email);
            return ResponseEntity.ok(Collections.singletonMap("message", "Token refreshed successfully"));
        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}, 이유: {}", email, e.getMessage());
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Token refresh failed: " + e.getMessage()));
        }
    }
}

