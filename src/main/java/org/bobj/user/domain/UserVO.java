package org.bobj.user.domain;

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
    private boolean isAdmin;      // is_admin BOOLEAN
    private String bankCode;      // bank_code VARCHAR(50)
    private String accountNumber; // account_number VARCHAR(50)


}