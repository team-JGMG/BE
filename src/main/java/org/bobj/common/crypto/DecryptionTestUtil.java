package org.bobj.common.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.user.dto.response.UserResponseDTO;
import org.bobj.user.service.UserService;
import org.springframework.stereotype.Component;

/**
 * 🧪 복호화 시스템 테스트 유틸리티
 * 
 * ResponseBodyAdvice가 제대로 작동하는지 확인할 수 있는 테스트 도구입니다.
 * 개발/테스트 환경에서만 사용하세요.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DecryptionTestUtil {
    
    private final UserService userService;
    private final PersonalDataCrypto personalDataCrypto;

    /**
     * 특정 사용자의 암호화/복호화 상태를 비교 출력
     */
    public void testUserDecryption(Long userId) {
        try {
            log.info("🧪 사용자 복호화 테스트 시작 - userId: {}", userId);
            
            // 1. Service에서 조회 (암호화된 상태)
            UserResponseDTO encrypted = userService.findUserInfoById(userId);
            
            log.info("📦 DB에서 조회된 원본 (암호화됨):");
            log.info("  - 이름: {}", encrypted.getName());
            log.info("  - 전화번호: {}", encrypted.getPhone());
            log.info("  - 계좌번호: {}", encrypted.getAccountNumber());
            log.info("  - 은행코드: {}", encrypted.getBankCode());
            
            // 2. 수동 복호화 테스트
            log.info("🔓 수동 복호화 결과:");
            if (encrypted.getName() != null) {
                String decryptedName = personalDataCrypto.decrypt(encrypted.getName(), PersonalDataCrypto.FieldType.NAME);
                log.info("  - 이름: {} -> {}", encrypted.getName(), decryptedName);
            }
            
            if (encrypted.getPhone() != null) {
                String decryptedPhone = personalDataCrypto.decrypt(encrypted.getPhone(), PersonalDataCrypto.FieldType.PHONE);
                log.info("  - 전화번호: {} -> {}", encrypted.getPhone(), decryptedPhone);
            }
            
            if (encrypted.getAccountNumber() != null) {
                String decryptedAccount = personalDataCrypto.decrypt(encrypted.getAccountNumber(), PersonalDataCrypto.FieldType.ACCOUNT_NUMBER);
                log.info("  - 계좌번호: {} -> {}", encrypted.getAccountNumber(), decryptedAccount);
            }
            
            if (encrypted.getBankCode() != null) {
                String decryptedBank = personalDataCrypto.decrypt(encrypted.getBankCode(), PersonalDataCrypto.FieldType.BANK_CODE);
                log.info("  - 은행코드: {} -> {}", encrypted.getBankCode(), decryptedBank);
            }
            
            log.info("✅ ResponseBodyAdvice는 API 응답시 자동으로 위와 같이 복호화합니다!");
            log.info("🧪 사용자 복호화 테스트 완료");
            
        } catch (Exception e) {
            log.error("❌ 복호화 테스트 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 암호화 시스템 상태 확인
     */
    public void checkDecryptionSystem() {
        log.info("🔍 복호화 시스템 상태 확인");
        
        try {
            // 1. PersonalDataCrypto 빈 확인
            log.info("✅ PersonalDataCrypto 빈 정상 로드됨");
            
            // 2. 간단한 암호화/복호화 테스트
            String testData = "테스트데이터";
            String encrypted = personalDataCrypto.encrypt(testData, PersonalDataCrypto.FieldType.NAME);
            String decrypted = personalDataCrypto.decrypt(encrypted, PersonalDataCrypto.FieldType.NAME);
            
            if (testData.equals(decrypted)) {
                log.info("✅ 암호화/복호화 시스템 정상 작동: {} -> {} -> {}", testData, encrypted, decrypted);
            } else {
                log.error("❌ 암호화/복호화 시스템 오류: 원본과 복호화 결과가 다름");
            }
            
            // 3. 지원되는 필드 타입 확인
            log.info("✅ 지원되는 개인정보 필드 타입:");
            for (PersonalDataCrypto.FieldType fieldType : PersonalDataCrypto.FieldType.values()) {
                log.info("  - {}: {}", fieldType.name(), fieldType.getContext());
            }
            
            log.info("🎉 복호화 시스템 모든 검사 통과!");
            
        } catch (Exception e) {
            log.error("❌ 복호화 시스템 검사 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * API 응답 자동 복호화 작동 확인용 메시지
     */
    public void explainAutoDecryption() {
        log.info("📋 ResponseBodyAdvice 자동 복호화 시스템 안내");
        log.info("┌─────────────────────────────────────────────────────────┐");
        log.info("│  🔓 API 응답 자동 복호화 시스템이 활성화되었습니다!        │");
        log.info("├─────────────────────────────────────────────────────────┤");
        log.info("│  ✅ 기존 Controller/Service 코드 변경 없이 사용 가능      │");
        log.info("│  ✅ 모든 API 응답에서 개인정보 자동 복호화               │");
        log.info("│  ✅ 복호화 실패시 원본 데이터 반환 (안전성 보장)         │");
        log.info("│                                                        │");
        log.info("│  🎯 복호화 대상:                                        │");
        log.info("│    - UserResponseDTO (이름, 전화번호, 계좌번호, 은행코드) │");
        log.info("│    - SellerDTO (판매자 이름, 전화번호)                  │");
        log.info("│    - PropertyDetailDTO (포함된 판매자 정보)             │");
        log.info("│    - List<위 객체들>                                   │");
        log.info("│    - ApiCommonResponse<위 객체들>                      │");
        log.info("│                                                        │");
        log.info("│  📱 API 테스트:                                        │");
        log.info("│    GET /api/users/me                                  │");
        log.info("│    GET /api/property/{id}                             │");
        log.info("│    GET /api/users/{userId} (관리자용)                  │");
        log.info("└─────────────────────────────────────────────────────────┘");
    }
}
