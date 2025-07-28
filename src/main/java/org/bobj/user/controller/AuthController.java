package org.bobj.user.controller;

import io.jsonwebtoken.Claims;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.user.domain.UserVO;
import org.bobj.user.dto.request.UserRegistrationRequestDTO;
import org.bobj.user.dto.response.AuthResponseDTO;
import org.bobj.user.dto.response.SimpleResponseDTO;
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
@Api(tags = "인증 및 회원가입 API")
public class AuthController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

//    @GetMapping("/oauth/kakao-url") //url 제공.
//    public ResponseEntity<Map<String, String>> getKakaoLoginUrl() {
//        String kakaoLoginUrl = "http://localhost:8080/oauth2/authorization/kakao";
//        return ResponseEntity.ok(Collections.singletonMap("url", kakaoLoginUrl));
//    }
    /**
     * 카카오 로그인 시작 API
     * 클라이언트가 이 API를 호출하면 카카오 로그인 URL을 받고,
     * 로그인 후 pre-auth 토큰을 받아서 추가 단계를 진행해야 합니다.
     */
    @GetMapping("/login/kakao")
    @ApiOperation(value = "카카오 로그인 시작", notes = "카카오 로그인 URL과 로그인 플로우 안내를 제공합니다. 로그인 완료 시 pre-auth 토큰이 HttpOnly 쿠키로 설정됩니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "카카오 로그인 URL 제공 성공", response = SimpleResponseDTO.class),
            @ApiResponse(code = 500, message = "서버 내부 오류\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 500,\n" +
                    "  \"code\": \"AUTH001\",\n" +
                    "  \"message\": \"카카오 로그인 설정 오류가 발생했습니다.\",\n" +
                    "  \"path\": \"/api/auth/login/kakao\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class)
    })
    public ResponseEntity<SimpleResponseDTO> startKakaoLogin() {
        String kakaoLoginUrl = "http://localhost:8080/oauth2/authorization/kakao";
        
        Map<String, Object> loginData = Map.of(
                "loginUrl", kakaoLoginUrl,
                "flow", Map.of(
                        "step1", "카카오 로그인 완료 후 pre-auth 토큰이 HttpOnly 쿠키로 설정됩니다.",
                        "step2", "쿠키의 pre-auth 토큰으로 /api/auth/login/callback API를 호출해서 사용자 존재 여부를 확인합니다.",
                        "step3a", "기존 사용자면 바로 최종 토큰을 받고 pre-auth 쿠키가 삭제됩니다.",
                        "step3b", "신규 사용자면 추가 정보 입력 후 /api/auth/signup API로 최종 회원가입 시 pre-auth 쿠키가 삭제됩니다."
                ),
                "callbackInfo", "로그인 완료 시 " + System.getProperty("custom.oauth2.redirect-uri", "http://localhost:8080/auth/callback") +
                               "?status=PRE_AUTH 형태로 리다이렉트 (토큰은 쿠키에 있음)"
        );
        
        return ResponseEntity.ok(SimpleResponseDTO.success("카카오 로그인 URL을 제공합니다.", loginData));
    }

    @GetMapping("/login/callback")
    @ApiOperation(value = "OAuth 콜백 처리", notes = "카카오 로그인 후 HttpOnly 쿠키의 pre-auth 토큰으로 사용자 존재 여부 확인 및 로그인 처리를 수행합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "기존 사용자 로그인 성공", response = AuthResponseDTO.class),
            @ApiResponse(code = 200, message = "신규 사용자 감지 (추가 정보 입력 필요)", response = SimpleResponseDTO.class),
            @ApiResponse(code = 401, message = "유효하지 않은 토큰\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 401,\n" +
                    "  \"code\": \"AUTH002\",\n" +
                    "  \"message\": \"유효하지 않은 토큰입니다.\",\n" +
                    "  \"path\": \"/api/auth/login/callback\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "소셜 로그인 정보 불일치\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 404,\n" +
                    "  \"code\": \"AUTH003\",\n" +
                    "  \"message\": \"해당 소셜 로그인 정보를 찾을 수 없습니다.\",\n" +
                    "  \"path\": \"/api/auth/login/callback\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 500,\n" +
                    "  \"code\": \"AUTH004\",\n" +
                    "  \"message\": \"OAuth 처리 중 서버 오류가 발생했습니다.\",\n" +
                    "  \"path\": \"/api/auth/login/callback\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class)
    })

    public ResponseEntity<?> checkUserExist(HttpServletRequest request, HttpServletResponse response) {
        log.info("=== OAuth Callback 시작 ===");
        
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("유효하지 않은 토큰으로 요청");
            return ResponseEntity.status(401).body(SimpleResponseDTO.error("유효하지 않은 토큰입니다."));
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
            AuthResponseDTO authResponseDTO = userService.loginExistingSocialUser(provider, providerId);
            // Access Token을 쿠키로 설정
            CookieUtil.setAccessTokenCookie(response, authResponseDTO.getAccessToken());
            // Pre-Auth Token 쿠키 삭제 (일회성 사용 완료)
            CookieUtil.deletePreAuthTokenCookie(response);
            
            log.info("소셜 로그인 처리 성공 - 토큰 발급 완료");

            return ResponseEntity.ok(authResponseDTO);
        } catch (UsernameNotFoundException e) {
            log.info("신규 사용자 감지 - email: {}", email);
            return ResponseEntity.ok(SimpleResponseDTO.success("신규 사용자입니다. 추가 정보를 입력해주세요.", 
                Map.of("exists", false, "email", email)));
        } catch (Exception e) {
            log.error("OAuth Callback 처리 중 예외 발생 - email: {}, provider: {}, providerId: {}", 
                email, provider, providerId, e);
            // 오류 발생시 pre-auth 토큰 삭제
            CookieUtil.deletePreAuthTokenCookie(response);
            throw e; // GlobalExceptionHandler가 처리하도록 다시 던짐
        }
    }

    @PostMapping("/signup")
    @ApiOperation(value = "OAuth 회원가입 완료", notes = "HttpOnly 쿠키의 pre-auth 토큰과 추가 정보를 받아 최종 회원가입을 완료합니다. 완료 후 pre-auth 쿠키가 삭제되고 access 토큰 쿠키가 설정됩니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "회원가입 성공", response = AuthResponseDTO.class),
            @ApiResponse(code = 400, message = "입력값 검증 실패 (실명, 주민등록번호, 휴대폰, 은행정보 등)\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 400,\n" +
                    "  \"code\": \"AUTH005\",\n" +
                    "  \"message\": \"실명은 필수입니다.\",\n" +
                    "  \"path\": \"/api/auth/signup\"\n" +
                    "}\n" +
                    "```\n\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 400,\n" +
                    "  \"code\": \"AUTH006\",\n" +
                    "  \"message\": \"휴대폰 번호 형식이 올바르지 않습니다.\",\n" +
                    "  \"path\": \"/api/auth/signup\"\n" +
                    "}\n" +
                    "```\n\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 400,\n" +
                    "  \"code\": \"AUTH007\",\n" +
                    "  \"message\": \"주민등록번호 형식이 올바르지 않습니다.\",\n" +
                    "  \"path\": \"/api/auth/signup\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 401, message = "유효하지 않은 사전 인증 토큰\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 401,\n" +
                    "  \"code\": \"AUTH008\",\n" +
                    "  \"message\": \"유효하지 않은 사전 인증 토큰입니다.\",\n" +
                    "  \"path\": \"/api/auth/signup\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 403, message = "올바르지 않은 토큰 타입\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 403,\n" +
                    "  \"code\": \"AUTH009\",\n" +
                    "  \"message\": \"올바르지 않은 토큰 타입입니다.\",\n" +
                    "  \"path\": \"/api/auth/signup\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 500,\n" +
                    "  \"code\": \"AUTH010\",\n" +
                    "  \"message\": \"회원가입 처리 중 서버 오류가 발생했습니다.\",\n" +
                    "  \"path\": \"/api/auth/signup\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class)
    })
    public ResponseEntity<?> registerFinal(@RequestBody @ApiParam(value = "회원가입 추가 정보", required = true) UserRegistrationRequestDTO registrationRequest,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        // 1. 사전 인증 토큰 검증
        String preAuthToken = jwtTokenProvider.resolveToken(request);
        if (preAuthToken == null || !jwtTokenProvider.validateToken(preAuthToken)) {
            // 유효하지 않은 토큰시 pre-auth 토큰 쿠키 삭제
            CookieUtil.deletePreAuthTokenCookie(response);
            return ResponseEntity.status(401).body(SimpleResponseDTO.error("유효하지 않은 사전 인증 토큰입니다."));
        }

        Claims claims = jwtTokenProvider.getClaims(preAuthToken);
        if (!"pre-auth".equals(claims.get("type", String.class))) {
            // 잘못된 토큰 타입시 pre-auth 토큰 삭제
            CookieUtil.deletePreAuthTokenCookie(response);
            return ResponseEntity.status(403).body(SimpleResponseDTO.error("올바르지 않은 토큰 타입입니다."));
        }

        // 2. 서버 사이드 검증 (클라이언트 검증 우회 방지)
        UserRegistrationRequestDTO.ValidationResult validationResult = registrationRequest.validate();
        if (!validationResult.isValid()) {
            log.warn("회원가입 요청 검증 실패: {}", validationResult.getErrors());
            // 검증 실패시 pre-auth 토큰 삭제
            CookieUtil.deletePreAuthTokenCookie(response);
            return ResponseEntity.badRequest().body(
                SimpleResponseDTO.error("입력값이 올바르지 않습니다.", 
                    Map.of("details", validationResult.getErrors()))
            );
        }

        try {
            AuthResponseDTO finalAuthResponse = userService.registerUserAndCreateFinalToken(registrationRequest, claims);
            // Access Token을 쿠키로 설정
            CookieUtil.setAccessTokenCookie(response, finalAuthResponse.getAccessToken());
            // Pre-Auth Token 쿠키 삭제 (회원가입 완료)
            CookieUtil.deletePreAuthTokenCookie(response);
            
            log.info("회원가입 성공: {}", registrationRequest.toMaskedString());
            return ResponseEntity.ok(finalAuthResponse);
        } catch (Exception e) {
            log.error("회원가입 처리 중 오류 발생", e);
            // 오류 발생시 pre-auth 토큰 삭제
            CookieUtil.deletePreAuthTokenCookie(response);
            return ResponseEntity.status(500).body(SimpleResponseDTO.error("회원가입 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 로그아웃 - 쿠키 삭제 및 Refresh Token DB에서 제거
     */
    @PostMapping("/oauth/logout")
    @ApiOperation(value = "로그아웃", notes = "액세스 토큰 쿠키 삭제 및 리프레시 토큰을 DB에서 제거합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "로그아웃 성공", response = SimpleResponseDTO.class),
            @ApiResponse(code = 500, message = "서버 내부 오류\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 500,\n" +
                    "  \"code\": \"AUTH011\",\n" +
                    "  \"message\": \"로그아웃 처리 중 서버 오류가 발생했습니다.\",\n" +
                    "  \"path\": \"/api/auth/oauth/logout\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class)
    })
    public ResponseEntity<SimpleResponseDTO> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = jwtTokenProvider.resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getUserPk(token);
            // Refresh Token DB에서 제거
            userService.removeRefreshToken(email);
            log.info("로그아웃 처리 완료: {}", email);
        }

        // 쿠키 삭제
        CookieUtil.deleteAccessTokenCookie(response);

        return ResponseEntity.ok(SimpleResponseDTO.success("로그아웃이 완료되었습니다."));
    }

    /**
     * Access Token 재발급
     * 클라이언트는 만료된 Access Token만 보내고, 서버에서 DB의 Refresh Token으로 갱신
     */
    @PostMapping("/oauth/token-refresh")
    @ApiOperation(value = "액세스 토큰 갱신", notes = "만료된 액세스 토큰을 새로운 토큰으로 갱신합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "토큰 갱신 성공", response = AuthResponseDTO.class),
            @ApiResponse(code = 401, message = "토큰 갱신 실패 (토큰 없음, 유효하지 않은 토큰, 리프레시 토큰 만료 등)\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 401,\n" +
                    "  \"code\": \"AUTH012\",\n" +
                    "  \"message\": \"토큰이 제공되지 않았습니다.\",\n" +
                    "  \"path\": \"/api/auth/oauth/token-refresh\"\n" +
                    "}\n" +
                    "```\n\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 401,\n" +
                    "  \"code\": \"AUTH013\",\n" +
                    "  \"message\": \"유효하지 않은 토큰입니다.\",\n" +
                    "  \"path\": \"/api/auth/oauth/token-refresh\"\n" +
                    "}\n" +
                    "```\n\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 401,\n" +
                    "  \"code\": \"AUTH014\",\n" +
                    "  \"message\": \"Refresh Token이 만료되었습니다. 다시 로그인해주세요.\",\n" +
                    "  \"path\": \"/api/auth/oauth/token-refresh\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": 500,\n" +
                    "  \"code\": \"AUTH015\",\n" +
                    "  \"message\": \"토큰 갱신 중 서버 오류가 발생했습니다.\",\n" +
                    "  \"path\": \"/api/auth/oauth/token-refresh\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class)
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
            CookieUtil.setAccessTokenCookie(response, newAuthResponse.getAccessToken());
            
            log.info("토큰 갱신 완료: {}", email);
            // AuthResponseDTO를 직접 반환 (일관성 있는 응답)
            return ResponseEntity.ok(newAuthResponse);
        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}, 이유: {}", email, e.getMessage());
            return ResponseEntity.status(401).body(SimpleResponseDTO.error("토큰 갱신에 실패했습니다: " + e.getMessage()));
        }
    }
}

