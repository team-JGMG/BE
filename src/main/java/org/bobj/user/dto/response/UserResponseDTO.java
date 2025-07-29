package org.bobj.user.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.user.domain.UserVO;
import org.bobj.user.security.UserPrincipal;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "사용자 정보 응답 DTO")
public class UserResponseDTO {

    @ApiModelProperty(value = "사용자 ID", example = "12345", required = true)
    private Long userId;

    @ApiModelProperty(value = "실명", example = "홍길동", required = true)
    private String name;

    @ApiModelProperty(value = "이메일", example = "user@example.com", required = true)
    private String email;

    @ApiModelProperty(value = "닉네임", example = "길동이", required = true)
    private String nickname;

    @ApiModelProperty(value = "휴대폰 번호", example = "01012345678", required = false)
    private String phone;

    @ApiModelProperty(value = "계좌번호", example = "123456789012", required = false)
    private String accountNumber;

    @ApiModelProperty(value = "은행명", example = "004", required = false)
    private String bankCode;

    @ApiModelProperty(value = "관리자 여부", example = "false", required = true)
    private Boolean isAdmin;

    @ApiModelProperty(value = "사용자 역할", example = "USER", allowableValues = "USER, ADMIN", required = true)
    private String role;

    @ApiModelProperty(value = "가입일시", example = "2025-07-27T15:30:00", required = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "수정일시", example = "2025-07-27T15:30:00", required = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // UserVO에서 생성 (DB 조회 결과)
    public UserResponseDTO(UserVO user) {
        if (user == null) return;
        
        this.userId = user.getUserId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.phone = user.getPhone();
        this.accountNumber = user.getAccountNumber();
        this.bankCode = user.getBankCode();
        this.isAdmin = user.getIsAdmin();
        this.role = user.getIsAdmin() ? "ADMIN" : "USER";
        this.createdAt = user.getCreatedAt() != null ? 
            user.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
        this.updatedAt = user.getUpdatedAt() != null ? 
            user.getUpdatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
    }

    // UserPrincipal에서 생성 (DB 조회 없음) - 성능 최적화
    public UserResponseDTO(UserPrincipal userPrincipal) {
        if (userPrincipal == null) return;
        
        this.userId = userPrincipal.getUserId();
        this.name = userPrincipal.getName();
        this.email = userPrincipal.getEmail();
        this.nickname = userPrincipal.getNickname();
        this.phone = null; // UserPrincipal에 없는 정보는 null
        this.accountNumber = null; // UserPrincipal에 없는 정보는 null
        this.bankCode = null; // UserPrincipal에 없는 정보는 null
        this.isAdmin = userPrincipal.isAdminUser();
        this.role = userPrincipal.getRole();
        this.createdAt = null; // UserPrincipal에 없는 정보
        this.updatedAt = null; // UserPrincipal에 없는 정보
    }

    /**
     * 정적 팩토리 메소드들
     */
    public static UserResponseDTO of(UserVO user) {
        return user == null ? null : new UserResponseDTO(user);
    }

    public static UserResponseDTO of(UserPrincipal userPrincipal) {
        return userPrincipal == null ? null : new UserResponseDTO(userPrincipal);
    }
}
