package org.bobj.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.common.crypto.DecryptionResponseAdvice;
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
    private final DecryptionResponseAdvice decryptionResponseAdvice;

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
            "복호화 테스트", "GET /api/dev/test/seller/{userId}",
            "수동 복호화", "POST /api/dev/crypto/decrypt",
            "펀딩 복호화 테스트", "GET /api/dev/test/funding/{fundingId} (NEW!)"
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
            response.put("isAdmin", user.isAdmin());
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
     * 🏪 SellerDTO 복호화 테스트 (개발용)
     */
    @GetMapping("/test/seller/{userId}")
    public ResponseEntity<Map<String, Object>> testSellerDecryption(@PathVariable Long userId) {
        try {
            // 사용자 정보 조회
            UserVO user = userService.findUserVOById(userId);
            
            // SellerDTO 생성 (암호화된 상태)
            org.bobj.property.dto.SellerDTO encryptedSeller = org.bobj.property.dto.SellerDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())       // 암호화된 상태
                .phone(user.getPhone())     // 암호화된 상태  
                .email(user.getEmail())     // 평문
                .build();
            
            // 🔓 수동 복호화 적용
            org.bobj.property.dto.SellerDTO decryptedSeller = decryptionResponseAdvice.decryptSellerDTOManual(encryptedSeller);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "🏪 SellerDTO 복호화 테스트 성공!");
            response.put("userId", userId);
            response.put("encrypted", Map.of(
                "name", encryptedSeller.getName(),
                "phone", encryptedSeller.getPhone(),
                "email", encryptedSeller.getEmail()
            ));
            response.put("decrypted", Map.of(
                "name", decryptedSeller.getName(),
                "phone", decryptedSeller.getPhone(),
                "email", decryptedSeller.getEmail()
            ));
            response.put("timestamp", LocalDateTime.now());
            response.put("note", "✨ Legacy Spring 환경에서 수동 복호화가 정상 작동합니다!");
            
            log.info("🏪 SellerDTO 복호화 테스트 성공 - userId: {}", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "❌ SellerDTO 복호화 테스트 실패");
            error.put("error", e.getMessage());
            error.put("userId", userId);
            error.put("timestamp", LocalDateTime.now());
            
            log.error("❌ SellerDTO 복호화 테스트 실패 - userId: {}", userId, e);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 🔧 수동 복호화 도구 (개발용)
     */
    @PostMapping("/crypto/decrypt")
    public ResponseEntity<Map<String, Object>> manualDecrypt(@RequestBody Map<String, String> request) {
        try {
            String encryptedData = request.get("data");
            String fieldTypeStr = request.get("fieldType");
            
            if (encryptedData == null || fieldTypeStr == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "❌ 필수 파라미터 누락");
                error.put("required", Map.of(
                    "data", "복호화할 암호화된 데이터",
                    "fieldType", "NAME, PHONE, ACCOUNT_NUMBER, BANK_CODE 중 하나"
                ));
                error.put("example", Map.of(
                    "data", "암호화된문자열==",
                    "fieldType", "NAME"
                ));
                return ResponseEntity.badRequest().body(error);
            }
            
            // PersonalDataCrypto 사용을 위해 수동으로 복호화
            org.bobj.common.crypto.PersonalDataCrypto.FieldType fieldType = 
                org.bobj.common.crypto.PersonalDataCrypto.FieldType.valueOf(fieldTypeStr.toUpperCase());
            
            String decryptedData = org.bobj.common.crypto.PersonalDataCrypto.decryptStatic(encryptedData, fieldType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "✅ 수동 복호화 성공");
            response.put("encrypted", encryptedData);
            response.put("decrypted", decryptedData);
            response.put("fieldType", fieldType.name());
            response.put("timestamp", LocalDateTime.now());
            
            log.info("🔧 수동 복호화 성공 - fieldType: {}", fieldType);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "❌ 수동 복호화 실패");
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            error.put("supportedFieldTypes", new String[]{"NAME", "PHONE", "ACCOUNT_NUMBER", "BANK_CODE", "SSN"});
            
            log.error("❌ 수동 복호화 실패", e);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 🏪 FundingDetailResponseDTO 복호화 테스트 (개발용)
     */
    @GetMapping("/test/funding/{fundingId}")
    public ResponseEntity<Map<String, Object>> testFundingDecryption(@PathVariable Long fundingId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "🏪 FundingDetailResponseDTO 복호화 테스트");
            response.put("fundingId", fundingId);
            response.put("timestamp", LocalDateTime.now());
            response.put("testNote", "실제 펀딩 API 호출: GET /api/funding/" + fundingId);
            response.put("expectedResult", "매도자 정보의 이름과 전화번호가 복호화되어 나타남");
            
            log.info("🏪 FundingDetailResponseDTO 복호화 테스트 안내 - fundingId: {}", fundingId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "❌ FundingDetailResponseDTO 복호화 테스트 실패");
            error.put("error", e.getMessage());
            error.put("fundingId", fundingId);
            error.put("timestamp", LocalDateTime.now());
            
            log.error("❌ FundingDetailResponseDTO 복호화 테스트 실패 - fundingId: {}", fundingId, e);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 📊 복호화 시스템 상태 확인
     */
    @GetMapping("/crypto/status")
    public ResponseEntity<Map<String, Object>> getCryptoStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("title", "🔓 복호화 시스템 상태");
        status.put("timestamp", LocalDateTime.now());
        
        status.put("responseBodyAdvice", Map.of(
            "status", "✅ 활성화됨",
            "description", "모든 API 응답에서 개인정보 자동 복호화",
            "package", "org.bobj.common.crypto.DecryptionResponseAdvice"
        ));
        
        status.put("supportedTypes", Map.of(
            "UserResponseDTO", "이름, 전화번호, 계좌번호, 은행코드",
            "SellerDTO", "판매자 이름, 전화번호",
            "PropertyDetailDTO", "포함된 판매자 정보",
            "FundingDetailResponseDTO", "펀딩 상세의 매도자 정보 (NEW!)"
        ));
        
        status.put("testEndpoints", Map.of(
            "SellerDTO 복호화", "GET /api/dev/test/seller/{userId}",
            "수동 복호화", "POST /api/dev/crypto/decrypt",
            "실제 매물 API", "GET /api/auth/property/{propertyId}",
            "실제 펀딩 API", "GET /api/funding/{fundingId} (NEW!)",
            "펀딩 복호화 테스트", "GET /api/dev/test/funding/{fundingId}"
        ));
        
        status.put("note", "✨ Legacy Spring 환경에서 수동 복호화가 정상 작동합니다! FundingDetailResponseDTO 지원 추가됨!");
        
        return ResponseEntity.ok(status);
    }
}
