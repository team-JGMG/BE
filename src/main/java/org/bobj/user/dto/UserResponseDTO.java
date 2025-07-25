package org.bobj.user.dto;

import lombok.Getter;
import org.bobj.user.domain.UserVO;

@Getter
public class UserResponseDTO {
    private Long userId;
    private String name;
    private String email;
    private String nickname;
    private String phone;
    private String accountNumber;
    private String bankName;
    private boolean isAdmin;    // 편의를 위해 유지
    private String role;        // 추가
    // 필요한 다른 안전한 정보 추가

    public UserResponseDTO(UserVO user) {
        this.userId = user.getUserId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.phone = user.getPhone();
        this.accountNumber = user.getAccountNumber();
        this.bankName = user.getBankCode();
        this.isAdmin = user.getIsAdmin();
        this.role = user.getIsAdmin() ? "ADMIN" : "USER";  // role 자동 설정
    }
}
