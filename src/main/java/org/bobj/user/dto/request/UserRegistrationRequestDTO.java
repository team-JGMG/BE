package org.bobj.user.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "회원가입 추가 정보 입력 요청 DTO")
public class UserRegistrationRequestDTO {

    @ApiModelProperty(value = "실명", example = "홍길동", required = true)
    private String name;

    @ApiModelProperty(value = "주민등록번호", example = "901010-1******", required = true)
    private String ssn;

    @ApiModelProperty(value = "휴대폰 번호", example = "01012345678", required = true)
    private String phone;

    @ApiModelProperty(value = "은행 코드", example = "004", required = true)
    private String bankCode;

    @ApiModelProperty(value = "계좌번호", example = "123456789012", required = true)
    private String accountNumber;

    /**
     * 입력값 유효성 검증 (서버 사이드 검증)
     * 클라이언트 검증을 우회한 요청도 서버에서 차단
     */
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();
        
        // 실명 검증
        if (name == null || name.trim().isEmpty()) {
            result.addError("name", "실명은 필수입니다");
        } else if (name.trim().length() < 2 || name.trim().length() > 20) {
            result.addError("name", "실명은 2-20자 사이여야 합니다");
        }
        
        // 주민등록번호 검증
        if (ssn == null || !ssn.matches("^\\d{13}$")) {
            result.addError("ssn", "주민등록번호 형식이 올바르지 않습니다");
        }
        
        // 휴대폰 번호 검증
        if (phone == null || !phone.matches("^01[0-9]\\d{7,8}$")) {
            result.addError("phone", "휴대폰 번호 형식이 올바르지 않습니다 (예: 01012345678)");
        }
        
        // 은행 코드 검증
        if (bankCode == null || bankCode.trim().isEmpty()) {
            result.addError("bankCode", "은행 코드는 필수입니다");
        } else if (bankCode.trim().length() < 2 || bankCode.trim().length() > 10) {
            result.addError("bankCode", "은행 코드는 3-10자 사이여야 합니다");
        }
        
        // 계좌번호 검증
        if (accountNumber == null || !accountNumber.matches("^\\d{10,20}$")) {
            result.addError("accountNumber", "계좌번호는 10-20자리 숫자여야 합니다");
        }
        
        return result;
    }

    /**
     * 민감정보 마스킹 (로깅용)
     */
    public String toMaskedString() {
        String maskedPhone = null;
        if (phone != null && phone.length() >= 11) {
            maskedPhone = phone.substring(0, 3) + "****" + phone.substring(7);
        } else if (phone != null) {
            maskedPhone = phone.substring(0, Math.min(3, phone.length())) + "****";
        }
        
        return String.format("UserRegistrationRequest{name='%s', phone='%s', bankCode='%s'}",
                name != null ? name.charAt(0) + "*".repeat(Math.max(0, name.length() - 1)) : null,
                maskedPhone,
                bankCode);
    }

    /**
     * 검증 결과를 담는 내부 클래스
     */
    @Data
    public static class ValidationResult {
        private java.util.Map<String, String> errors = new java.util.HashMap<>();
        
        public void addError(String field, String message) {
            errors.put(field, message);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public java.util.Map<String, String> getErrors() {
            return errors;
        }
    }
}
