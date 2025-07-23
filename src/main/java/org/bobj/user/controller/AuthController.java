package org.bobj.user.controller;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.bobj.user.dto.AdditionalInfoDTO;
import org.bobj.user.dto.TokenDTO;
import org.bobj.user.security.JwtTokenProvider;
import org.bobj.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth") // API 경로를 /api/auth로 통일하는 것을 권장합니다.
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/kakao-login-url")
    public ResponseEntity<Map<String, String>> getKakaoLoginUrl() {
        // Kakao OAuth2 인증 URL을 직접 작성
        String kakaoLoginUrl = "http://localhost:8080/oauth2/authorization/kakao";
        return ResponseEntity.ok(Collections.singletonMap("url", kakaoLoginUrl));
    }

    @GetMapping("/check-user")
    public ResponseEntity<Map<String, Object>> checkUserExist(HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Invalid token"));
        }

        Claims claims = jwtTokenProvider.getClaims(token);
        String email = claims.get("email", String.class);

        boolean exists = userService.findUserInfoByEmail(email).isPresent();

        // 토큰의 email 정보와 존재 여부를 함께 리턴
        Map<String, Object> response = Map.of(
                "exists", exists,
                "email", email
        );

        return ResponseEntity.ok(response);
    }


//  @param additionalInfoDTO 사용자가 입력한 추가 정보 @param request HTTP 요청 객체 (Header에서 토큰을 읽기 위해 필요)@return 최종 토큰 DTO
    @PostMapping("/register-final")
    public ResponseEntity<TokenDTO> registerFinal(@RequestBody AdditionalInfoDTO additionalInfoDTO, HttpServletRequest request) {
        // 1. Header에서 '사전 인증' 토큰을 추출합니다.
        String preAuthToken = jwtTokenProvider.resolveToken(request);
        if (preAuthToken == null || !jwtTokenProvider.validateToken(preAuthToken)) {
            return ResponseEntity.status(401).body(null); // 유효하지 않은 토큰
        }
        // 2. 토큰에서 클레임(소셜 정보)을 추출합니다.
        Claims claims = jwtTokenProvider.getClaims(preAuthToken);
        if (!"pre-auth".equals(claims.get("type", String.class))) {
            return ResponseEntity.status(403).body(null); // 토큰 타입이 올바르지 않음
        }
        // 3. UserService를 호출하여 최종 회원가입을 진행하고, 최종 토큰을 받습니다.
        TokenDTO finalTokenDTO = userService.registerUserAndCreateFinalToken(additionalInfoDTO, claims);

        return ResponseEntity.ok(finalTokenDTO);
    }
}
