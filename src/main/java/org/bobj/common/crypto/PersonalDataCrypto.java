package org.bobj.common.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

/**
 * 개인정보 암호화/복호화 유틸리티 클래스
 * AES-256-GCM 알고리즘 사용 (금융권 표준)
 */
@Slf4j
@Component
public class PersonalDataCrypto {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_LENGTH = 256;
    private static final int PBKDF2_ITERATIONS = 100000;

    // Spring DI용 인스턴스 필드
    @Value("${app.crypto.master-key}")
    private String masterKey;

    // 정적 메서드용 마스터 키 (TypeHandler에서 사용)
    private static String staticMasterKey;

    // 정적 초기화 블록
    static {
        try {
            // 환경변수 우선 확인
            staticMasterKey = System.getenv("CRYPTO_MASTER_KEY");

            if (staticMasterKey == null || staticMasterKey.trim().isEmpty()) {
                // System Property 확인
                staticMasterKey = System.getProperty("app.crypto.master-key");
            }

            if (staticMasterKey == null || staticMasterKey.trim().isEmpty()) {
                // application.properties에서 읽기 시도
                Properties props = new Properties();
                InputStream is = PersonalDataCrypto.class.getResourceAsStream("/application.properties");
                if (is != null) {
                    props.load(is);
                    String propValue = props.getProperty("app.crypto.master-key");
                    if (propValue != null && propValue.startsWith("${")) {
                        // 변수 치환이 안된 경우 기본값 사용
                        staticMasterKey = "QmxvY2tjaGFpbjIwMjVTZWN1cml0eVByaXZhdGVLZXlGb3JQZXJzb25hbERhdGFFbmNyeXB0aW9u";
                    } else {
                        staticMasterKey = propValue;
                    }
                    is.close();
                }
            }

            // 최종 기본값 설정
            if (staticMasterKey == null || staticMasterKey.trim().isEmpty()) {
                staticMasterKey = "QmxvY2tjaGFpbjIwMjVTZWN1cml0eVByaXZhdGVLZXlGb3JQZXJzb25hbERhdGFFbmNyeXB0aW9u";
            }

            log.info("개인정보 암호화 시스템 정적 초기화 완료 - 키 길이: {}", staticMasterKey.length());

        } catch (Exception e) {
            log.error("개인정보 암호화 시스템 초기화 실패", e);
            staticMasterKey = "QmxvY2tjaGFpbjIwMjVTZWN1cml0eVByaXZhdGVLZXlGb3JQZXJzb25hbERhdGFFbmNyeXB0aW9u";
        }
    }

    /**
     * TypeHandler에서 사용하는 정적 메서드 - 암호화
     */
    public static String encryptStatic(String plainText, FieldType fieldType) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return plainText;
        }

        try {
            SecretKey secretKey = deriveKeyStatic(staticMasterKey, fieldType.getContext());
            byte[] iv = generateSecureIVStatic();

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);

            return Base64.getEncoder().encodeToString(result);

        } catch (Exception e) {
            log.error("개인정보 암호화 실패: fieldType={}, error={}", fieldType, e.getMessage(), e);
            throw new CryptoException("개인정보 암호화에 실패했습니다.", e);
        }
    }

    /**
     * TypeHandler에서 사용하는 정적 메서드 - 복호화
     */
    public static String decryptStatic(String encryptedData, FieldType fieldType) {
        if (encryptedData == null || encryptedData.trim().isEmpty()) {
            return encryptedData;
        }

        try {
            byte[] data = Base64.getDecoder().decode(encryptedData);

            byte[] iv = Arrays.copyOfRange(data, 0, GCM_IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(data, GCM_IV_LENGTH, data.length);

            SecretKey secretKey = deriveKeyStatic(staticMasterKey, fieldType.getContext());

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] decryptedData = cipher.doFinal(encrypted);

            return new String(decryptedData, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("개인정보 복호화 실패: fieldType={}, error={}", fieldType, e.getMessage(), e);
            throw new CryptoException("개인정보 복호화에 실패했습니다.", e);
        }
    }

    // Spring DI용 인스턴스 메서드들 (기존 코드와 동일)
    public String encrypt(String plainText, FieldType fieldType) {
        return encryptStatic(plainText, fieldType);
    }

    public String decrypt(String encryptedData, FieldType fieldType) {
        return decryptStatic(encryptedData, fieldType);
    }

    // 정적 헬퍼 메서드들
    private static SecretKey deriveKeyStatic(String masterKey, String context) throws Exception {
        String saltString = context + "_SALT_2025_BOBJ_SECURITY";
        byte[] salt = saltString.getBytes(StandardCharsets.UTF_8);

        PBEKeySpec keySpec = new PBEKeySpec(
                masterKey.toCharArray(),
                salt,
                PBKDF2_ITERATIONS,
                KEY_LENGTH
        );

        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();

        keySpec.clearPassword();

        return new SecretKeySpec(keyBytes, "AES");
    }

    private static byte[] generateSecureIVStatic() {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            return iv;
        } catch (Exception e) {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            return iv;
        }
    }

    /**
     * 개인정보 필드 타입 열거형
     */
    public enum FieldType {
        NAME("USER_NAME"),
        SSN("USER_SSN"),
        PHONE("USER_PHONE"),
        ACCOUNT_NUMBER("USER_ACCOUNT"),
        BANK_CODE("USER_BANK");

        private final String context;

        FieldType(String context) {
            this.context = context;
        }

        public String getContext() {
            return context;
        }
    }

    /**
     * 암호화 관련 예외 클래스
     */
    public static class CryptoException extends RuntimeException {
        public CryptoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}