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
    // 필요한 다른 안전한 정보 추가

    public UserResponseDTO(UserVO user) {
        this.userId = user.getUserId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.phone = user.getPhone();
    }
}
