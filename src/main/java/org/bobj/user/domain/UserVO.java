package org.bobj.user.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long userId;          // user_id BIGINT
    private String name;          // name VARCHAR(100)
    private String email;         // email VARCHAR(255)
    private String nickname;      // nickname VARCHAR(50)
    private String ssn;           // ssn VARCHAR(20) (주민등록번호)
    private String phone;         // phone VARCHAR(20)
    private Date createdAt;       // created_at DATETIME
    private Date updatedAt;       // updated_at DATETIME
    
    @JsonProperty("isAdmin")      // ← JSON 응답에서는 "isAdmin"으로 표시
    private boolean admin;        // ← 필드명 변경: isAdmin → admin
    
    private String bankCode;      // bank_code VARCHAR(50)
    private String accountNumber; // account_number VARCHAR(50)

    // ✅ 기존 코드 호환성을 위한 메서드 (deprecated)
    @Deprecated
    public boolean getIsAdmin() {
        return admin;
    }
    
    @Deprecated  
    public void setIsAdmin(boolean admin) {
        this.admin = admin;
    }
}