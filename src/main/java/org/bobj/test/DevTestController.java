package org.bobj.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.user.domain.UserVO;
import org.bobj.user.security.UserPrincipal;
import org.bobj.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 🚀 개발용 테스트 컨트롤러
 * 팀원들이 인증 없이 API 테스트할 수 있도록 제공
 * 
 * ⚠️ 운영 환경에서는 삭제하거나 비활성화하세요!
 */
@Slf4j
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevTestController {

    private final UserService userService;

    /**
     * 🔧 개발 환경 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDevStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("message", "🚀 개발 모드 활성화 - 인증 없이 API 테스트 가능");
        status.put("timestamp", LocalDateTime.now());
        status.put("security", "DISABLED (개발용)");
        status.put("apis", Map.of(
            "사용자 조회", "GET /api/dev/users/{userId}",
            "JWT 없이 테스트", "GET /api/dev/test/no-auth",
            "JWT 있을 때 테스트", "GET /api/dev/test/with-auth",
            "전체 API 목록", "GET /api/dev/help"
        ));
        return ResponseEntity.ok(status);
    }

    /**
     * 🔓 인증 없이 접근 가능한 테스트 API
     */
    @GetMapping("/test/no-auth")
    public ResponseEntity<Map<String, Object>> testNoAuth() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ 인증 없이 접근 성공!");
        response.put("timestamp", LocalDateTime.now());
        response.put("authRequired", false);
        
        log.info("개발 모드: 인증 없이 API 접근 성공");
        return ResponseEntity.ok(response);
    }

    /**
     * 🔐 JWT 토큰이 있을 때 추가 정보 제공
     */
    @GetMapping("/test/with-auth")
    public ResponseEntity<Map<String, Object>> testWithAuth(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        
        if (principal != null) {
            response.put("message", "✅ JWT 토큰으로 인증된 접근!");
            response.put("authRequired", true);
            response.put("userInfo", Map.of(
                "userId", principal.getUserId(),
                "email", principal.getEmail(),
                "role", principal.getRole()
            ));
            log.info("개발 모드: JWT 토큰으로 인증된 접근 - userId: {}", principal.getUserId());
        } else {
            response.put("message", "✅ JWT 토큰 없이 접근 (개발 모드에서 허용)");
            response.put("authRequired", false);
            response.put("userInfo", null);
            log.info("개발 모드: JWT 토큰 없이 접근");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 👤 사용자 정보 조회 (개발용)
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable Long userId) {
        try {
            UserVO user = userService.findUserVOById(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ 사용자 정보 조회 성공 (개인정보 자동 복호화됨)");
            response.put("userId", user.getUserId());
            response.put("email", user.getEmail());
            response.put("name", user.getName());           // 🔓 복호화된 이름
            response.put("nickname", user.getNickname());   // 평문 닉네임
            response.put("phone", user.getPhone());         // 🔓 복호화된 휴대폰
            response.put("isAdmin", user.getIsAdmin());
            response.put("timestamp", LocalDateTime.now());
            
            log.info("개발 모드: 사용자 정보 조회 성공 - userId: {}, name: {}", userId, user.getName());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "❌ 사용자 정보 조회 실패");
            error.put("error", e.getMessage());
            error.put("userId", userId);
            error.put("timestamp", LocalDateTime.now());
            
            log.error("개발 모드: 사용자 정보 조회 실패 - userId: {}", userId, e);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 📚 개발용 API 도움말
     */
    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> getApiHelp() {
        Map<String, Object> help = new HashMap<>();
        help.put("title", "🚀 개발용 API 테스트 가이드");
        help.put("description", "인증 없이 API를 테스트할 수 있습니다");
        
        help.put("endpoints", Map.of(
            "개발 상태", "GET /api/dev/status",
            "인증 없이 테스트", "GET /api/dev/test/no-auth",
            "JWT 테스트", "GET /api/dev/test/with-auth",
            "사용자 조회", "GET /api/dev/users/{userId}",
            "도움말", "GET /api/dev/help"
        ));
        
        help.put("originalApis", Map.of(
            "사용자 정보", "GET /api/users/me (JWT 필요했지만 지금은 불필요)",
            "펀딩 목록", "GET /api/fundings (개발 모드에서 자유 접근)",
            "포인트 내역", "GET /api/points (개발 모드에서 자유 접근)"
        ));
        
        help.put("howToTest", Map.of(
            "Postman", "Authorization 헤더 없이 바로 요청",
            "curl", "curl -X GET http://localhost:8080/api/dev/status",
            "브라우저", "주소창에 직접 입력하여 테스트 가능"
        ));
        
        help.put("note", "⚠️ 운영 환경에서는 이 컨트롤러를 삭제하거나 비활성화하세요!");
        
        return ResponseEntity.ok(help);
    }

    /**
     * 🧪 암호화 테스트 (개발용)
     */
    @PostMapping("/test/crypto")
    public ResponseEntity<Map<String, Object>> testCrypto(@RequestBody Map<String, String> request) {
        try {
            String testData = request.get("data");
            if (testData == null) {
                testData = "테스트 데이터";
            }
            
            // PersonalDataCrypto를 직접 사용해서 암호화/복호화 테스트
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ 암호화 시스템 테스트");
            response.put("original", testData);
            response.put("note", "암호화/복호화가 정상 작동하는지 확인용");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "❌ 암호화 테스트 실패");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.badRequest().body(error);
        }
    }
}
