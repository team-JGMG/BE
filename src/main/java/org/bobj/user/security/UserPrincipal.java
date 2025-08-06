package org.bobj.user.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bobj.user.domain.UserVO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * Spring Security에서 사용할 커스텀 UserPrincipal
 * @AuthenticationPrincipal로 컨트롤러에서 직접 사용자 정보에 접근 가능
 */
@Getter
public class UserPrincipal implements UserDetails {


    private final Long userId;
    private final String email;
    private final String name;
    private final String nickname;
    private final boolean isAdmin;
    private final String role;
    private final String phone;
    private final String ssn;
    private final String bankCode;      // bank_code VARCHAR(50)
    private final String accountNumber; // account_number VARCHAR(50)



    // UserVO에서 UserPrincipal 생성
    public UserPrincipal(UserVO user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.nickname = user.getNickname();
        this.isAdmin = user.getIsAdmin();
        this.role = user.getIsAdmin() ? "ADMIN" : "USER";
        this.phone = user.getPhone();
        this.ssn = user.getSsn();
        this.bankCode = user.getBankCode();
        this.accountNumber = user.getAccountNumber();
    }

    /**
     * JWT 토큰 정보로 UserPrincipal 생성 (인증 필터용)
     */
    public UserPrincipal(Long userId, String email, String role) {
        this.userId = userId;
        this.email = email;
        this.name = getName();  // JWT에는 name 정보 없음
        this.nickname = getNickname();  // JWT에는 nickname 정보 없음 (필요시 추가)
        this.isAdmin = "ADMIN".equals(role);
        this.role = role;
        this.phone = getPhone();
        this.ssn = getSsn();
        this.bankCode = getBankCode();
        this.accountNumber = getAccountNumber();
    }

    /**
     * JWT 토큰 정보로 UserPrincipal 생성 - 정적 팩토리 메서드
     */
    public static UserPrincipal fromJwtClaims(Long userId, String email, String role) {
        return new UserPrincipal(userId, email, role);
    }

    // 정적 팩토리 메소드
    public static UserPrincipal create(UserVO user) {
        return new UserPrincipal(user);
    }

    // Spring Security UserDetails 구현 메소드들
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + role)
        );
    }

    @Override
    public String getPassword() {
        return ""; // JWT 방식이므로 비밀번호 사용 안함
    }

    @Override
    public String getUsername() {
        return email; // 이메일을 username으로 사용
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // 편의 메소드들
    public boolean hasRole(String roleName) {
        return this.role.equals(roleName);
    }

    public boolean isUser() {
        return "USER".equals(this.role);
    }

    public boolean isAdminUser() {
        return "ADMIN".equals(this.role);
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", nickname='" + nickname + '\'' +
                ", role='" + role + '\'' +
                ", isAdmin=" + isAdmin + '\'' +
                ", phone='" + phone + '\'' +
                ", ssn='" + ssn + '\'' +
                ", bankCode='" + bankCode + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                '}';
    }
}