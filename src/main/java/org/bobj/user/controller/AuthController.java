package org.bobj.user.controller;

import io.jsonwebtoken.Claims;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.user.domain.UserVO;
import org.bobj.user.dto.request.LogoutRequestDTO;
import org.bobj.user.dto.request.UserRegistrationRequestDTO;
import org.bobj.user.dto.response.AuthResponseDTO;
import org.bobj.user.dto.response.SimpleResponseDTO;
import org.bobj.user.security.JwtTokenProvider;
import org.bobj.user.security.UserPrincipal;
import org.bobj.user.service.LogoutService;
import org.bobj.user.service.UserService;
import org.bobj.user.util.CookieUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Api(tags = "인증 및 회원가입 API")
public class AuthController {
    private final UserService userService;
    private final LogoutService logoutService;

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;

    @Value("${server.domain}")
    private String serverDomain;

    /**
     * 카카오 로그인 시작 API
     * 클라이언트가 이 API를 호출하면 카카오 로그인 URL을 받고,
     * 로그인 후 pre-auth or access 토큰을 받아서 추가 단계를 진행해야 합니다.
     */
    @GetMapping("/login/kakao")
    @ApiOperation(value = "카카오 로그인 시작", notes = "카카오 로그인 URL과 로그인 플로우 안내를 제공합니다. 로그인 완료 시 토큰이 HttpOnly 쿠키로 설정됩니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "카카오 로그인 URL 제공 성공\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"message\": \"카카오 로그인 URL을 제공합니다.\",\n" +
                    "  \"success\": true,\n" +
                    "  \"data\": {\n" +
                    "    \"loginUrl\": \"https://half-to-half.site/oauth2/authorization/kakao\",\n" +
                    "    \"flow\": {\n" +
                    "      \"step1\": \"카카오 로그인 완료 후 pre-auth 토큰이 HttpOnly 쿠키로 설정됩니다.\",\n" +
                    "      \"step2a\": \"기존 사용자면 바로 최종 토큰을 받습니다.\",\n" +
                    "      \"step2b\": \"신규 사용자면 추가 정보 입력 후 /api/auth/signup API로 최종 회원가입\"\n" +
                    "    },\n" +
                    "    \"callbackInfo\": \"로그인 완료 시 리다이렉트\"\n" +
                    "  },\n" +
                    "  \"timestamp\": \"2025-08-06 12:30:45\"\n" +
                    "}\n" +
                    "```", response = SimpleResponseDTO.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<SimpleResponseDTO> startKakaoLogin() {
        String kakaoLoginUrl = serverDomain + "/oauth2/authorization/kakao";

        Map<String, Object> loginData = Map.of(
                "loginUrl", kakaoLoginUrl,
                "flow", Map.of(
                        "step1", "카카오 로그인 완료 후 pre-auth 토큰이 HttpOnly 쿠키로 설정됩니다.",
                        "step2a", "기존 사용자면 바로 최종 토큰을 받습니다.",
                        "step2b", "신규 사용자면 추가 정보 입력 후 /api/auth/signup API로 최종 회원가입 시 pre-auth 쿠키가 삭제되고 최종 토큰 발급."
                ),
                "callbackInfo", "로그인 완료 시 " + System.getProperty("custom.oauth2.redirect-uri", "http://localhost:5173/auth/callback") +
                               "?status=PRE_AUTH 형태로 리다이렉트 (토큰은 쿠키에 있음)"
        );

        return ResponseEntity.ok(SimpleResponseDTO.success("카카오 로그인 URL을 제공합니다.", loginData));
    }



    @PostMapping("/signup")
    @ApiOperation(value = "OAuth 회원가입 완료", notes = "HttpOnly 쿠키의 pre-auth 토큰과 추가 정보를 받아 최종 회원가입을 완료합니다. 완료 후 pre-auth 쿠키가 삭제되고 access 토큰 쿠키가 설정됩니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "회원가입 성공\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"accessToken\": \"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNjkwMzc4ODAwLCJleHAiOjE2OTAzODA2MDB9.abcd1234...\",\n" +
                    "  \"refreshToken\": \"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNjkwMzc4ODAwLCJleHAiOjE2OTA0NjUyMDB9.efgh5678...\",\n" +
                    "  \"userId\": 12345,\n" +
                    "  \"isAdmin\": false,\n" +
                    "  \"role\": \"USER\",\n" +
                    "  \"tokenType\": \"Bearer\",\n" +
                    "  \"expiresIn\": 1800,\n" +
                    "  \"issuedAt\": \"2025-08-06 12:30:45\"\n" +
                    "}\n" +
                    "```", response = AuthResponseDTO.class),
            @ApiResponse(code = 400, message = "입력값 검증 실패\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"message\": \"입력값이 올바르지 않습니다.\",\n" +
                    "  \"success\": false,\n" +
                    "  \"data\": {\n" +
                    "    \"details\": [\"실명은 필수입니다.\", \"휴대폰 번호 형식이 올바르지 않습니다.\"]\n" +
                    "  },\n" +
                    "  \"timestamp\": \"2025-08-06 12:30:45\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 401, message = "유효하지 않은 사전 인증 토큰", response = ErrorResponse.class),
            @ApiResponse(code = 403, message = "올바르지 않은 토큰 타입", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<?> registerFinal(@RequestBody @ApiParam(value = "회원가입 추가 정보", required = true) UserRegistrationRequestDTO registrationRequest,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        
        log.info("[회원가입 시작] 요청 데이터: {}", registrationRequest.toMaskedString());
        
        //모든 쿠키 로그 출력 (디버깅용)
        cookieUtil.logAllCookies(request);
        
        // 1. 사전 인증 토큰 검증
        String preAuthToken = jwtTokenProvider.resolveToken(request);
        log.info("[토큰 추출] preAuthToken 존재 여부: {}", preAuthToken != null);
        
        if (preAuthToken == null) {
            log.error("[토큰 오류] preAuthToken이 null입니다.");
            log.error("[상세 확인] 요청 헤더 Authorization: {}", request.getHeader("Authorization"));
            
            // 유효하지 않은 토큰시 pre-auth 토큰 쿠키 삭제
            cookieUtil.deletePreAuthTokenCookie(response, request);
            return ResponseEntity.status(401).body(SimpleResponseDTO.error("사전 인증 토큰이 없습니다."));
        }
        
        // 토큰 일부만 로그 출력 (보안)
        String tokenPreview = preAuthToken.length() > 20 ? 
            preAuthToken.substring(0, 20) + "..." : preAuthToken;
        log.info("[토큰 내용] preAuthToken 미리보기: {}", tokenPreview);
        
        if (!jwtTokenProvider.validateToken(preAuthToken)) {
            log.error("[토큰 검증] preAuthToken이 유효하지 않습니다: {}", tokenPreview);
            cookieUtil.deletePreAuthTokenCookie(response, request);
            return ResponseEntity.status(401).body(SimpleResponseDTO.error("유효하지 않은 사전 인증 토큰입니다."));
        }
        
        log.info("[토큰 검증] preAuthToken 유효성 검증 통과");

        Claims claims = jwtTokenProvider.getClaims(preAuthToken);
        String tokenType = claims.get("type", String.class);
        log.info("[토큰 타입] {}", tokenType);
        
        if (!"pre-auth".equals(tokenType)) {
            log.error("[토큰 타입 오류] 예상: pre-auth, 실제: {}", tokenType);
            cookieUtil.deletePreAuthTokenCookie(response, request);
            return ResponseEntity.status(403).body(SimpleResponseDTO.error("올바르지 않은 토큰 타입입니다."));
        }
        
        log.info("[토큰 타입] pre-auth 타입 확인 완료");

        // 2. 서버 사이드 검증 (클라이언트 검증 우회 방지)
        UserRegistrationRequestDTO.ValidationResult validationResult = registrationRequest.validate();
        if (!validationResult.isValid()) {
            log.warn("[검증 실패] 회원가입 요청 검증 실패: {}", validationResult.getErrors());
            cookieUtil.deletePreAuthTokenCookie(response, request);
            return ResponseEntity.badRequest().body(
                SimpleResponseDTO.error("입력값이 올바르지 않습니다.", 
                    Map.of("details", validationResult.getErrors()))
            );
        }
        
        log.info("[입력값 검증] 회원가입 데이터 검증 통과");

        try {
            log.info("[회원가입 처리] 최종 회원가입 시작 - 사용자: {}", claims.getSubject());
            AuthResponseDTO finalAuthResponse = userService.registerUserAndCreateFinalToken(registrationRequest, claims);
            
            // Access Token을 쿠키로 설정
            cookieUtil.setAccessTokenCookie(response, request, finalAuthResponse.getAccessToken());
            // Pre-Auth Token 쿠키 삭제 (회원가입 완료)
            cookieUtil.deletePreAuthTokenCookie(response, request);
            
            log.info("[회원가입 성공] 사용자: {}", claims.getSubject());
            return ResponseEntity.ok(finalAuthResponse);
            
        } catch (Exception e) {
            log.error("❌ [회원가입 실패] 처리 중 오류 발생", e);
            log.error("❌ [오류 상세] 메시지: {}", e.getMessage());
            log.error("❌ [오류 타입] {}", e.getClass().getSimpleName());
            
            // 오류 발생시 pre-auth 토큰 삭제
            cookieUtil.deletePreAuthTokenCookie(response, request);
            return ResponseEntity.status(500).body(SimpleResponseDTO.error("회원가입 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 로그아웃 - 쿠키 삭제 및 Refresh Token DB에서 제거
     */
    @PostMapping("/auth/logout")
    @ApiOperation(value = "로그아웃", notes = "액세스 토큰 쿠키 삭제 및 리프레시 토큰을 DB에서 제거합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "로그아웃 성공\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"message\": \"로그아웃이 완료되었습니다.\",\n" +
                    "  \"success\": true,\n" +
                    "  \"timestamp\": \"2025-08-06 12:30:45\"\n" +
                    "}\n" +
                    "```", response = SimpleResponseDTO.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<SimpleResponseDTO> logout(HttpServletRequest request, HttpServletResponse response,
                                                    @RequestBody LogoutRequestDTO logoutRequestDTO,
                                                    @ApiIgnore @AuthenticationPrincipal UserPrincipal principal) {
        String token = jwtTokenProvider.resolveToken(request);

        Long userId = principal.getUserId();

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getUserPk(token);
            // Refresh Token DB에서 제거
//            userService.removeRefreshToken(email);
            logoutService.processLogout(userId, email, logoutRequestDTO.getDeviceToken());
            log.info("로그아웃 처리 완료: {}", email);
        }

        // 쿠키 삭제
        cookieUtil.deleteAccessTokenCookie(response, request);

        return ResponseEntity.ok(SimpleResponseDTO.success("로그아웃이 완료되었습니다."));
    }

    /**
     * Access Token 재발급
     * 클라이언트는 만료된 Access Token만 보내고, 서버에서 DB의 Refresh Token으로 갱신
     */
    @PostMapping("/auth/token-refresh")
    @ApiOperation(value = "액세스 토큰 갱신", notes = "만료된 액세스 토큰을 새로운 토큰으로 갱신합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "토큰 갱신 성공\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"accessToken\": \"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNjkwMzc4ODAwLCJleHAiOjE2OTAzODA2MDB9.newtoken1234...\",\n" +
                    "  \"refreshToken\": \"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNjkwMzc4ODAwLCJleHAiOjE2OTA0NjUyMDB9.efgh5678...\",\n" +
                    "  \"userId\": 12345,\n" +
                    "  \"isAdmin\": false,\n" +
                    "  \"role\": \"USER\",\n" +
                    "  \"tokenType\": \"Bearer\",\n" +
                    "  \"expiresIn\": 1800,\n" +
                    "  \"issuedAt\": \"2025-08-06 12:30:45\"\n" +
                    "}\n" +
                    "```", response = AuthResponseDTO.class),
            @ApiResponse(code = 401, message = "토큰 갱신 실패\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"message\": \"토큰이 제공되지 않았습니다.\",\n" +
                    "  \"success\": false,\n" +
                    "  \"timestamp\": \"2025-08-06 12:30:45\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })

    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        log.info("토큰 갱신 요청 시작");
        
        // 쿠키에서 만료된 access token 추출
        String expiredAccessToken = jwtTokenProvider.resolveToken(request);
        
        if (expiredAccessToken == null) {
            log.warn("토큰이 제공되지 않음");
            return ResponseEntity.status(401).body(SimpleResponseDTO.error("토큰이 제공되지 않았습니다."));
        }
        
        // 만료된 토큰에서 사용자 정보 추출
        String email = jwtTokenProvider.getUserEmailFromExpiredToken(expiredAccessToken);
        if (email == null) {
            log.warn("토큰에서 이메일 추출 실패");
            return ResponseEntity.status(401).body(SimpleResponseDTO.error("유효하지 않은 토큰입니다."));
        }
        
        log.info("토큰 갱신 대상 사용자: {}", email);
        
        try {
            // DB에서 해당 사용자의 refresh token으로 새 access token 발급
            AuthResponseDTO newAuthResponse = userService.refreshAccessTokenByEmail(email);
            // 새로운 Access Token을 쿠키로 설정
            cookieUtil.setAccessTokenCookie(response, request, newAuthResponse.getAccessToken());
            
            log.info("토큰 갱신 완료: {}", email);
            // AuthResponseDTO를 직접 반환 (일관성 있는 응답)
            return ResponseEntity.ok(newAuthResponse);
        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}, 이유: {}", email, e.getMessage());
            return ResponseEntity.status(401).body(SimpleResponseDTO.error("토큰 갱신에 실패했습니다: " + e.getMessage()));
        }
    }
}

