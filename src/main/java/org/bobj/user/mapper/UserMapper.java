package org.bobj.user.mapper;

import org.bobj.user.domain.SocialLoginsVO;
import org.bobj.user.domain.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    // USERS 테이블 관련
    UserVO findUserByEmail(String email);
    UserVO findUserById(Long userId);
    void saveUser(UserVO user);
    void updateUser(UserVO user);

    // SOCIAL_LOGINS 테이블 관련
    SocialLoginsVO findSocialLoginByProviderAndProviderId(@Param("provider") String provider, @Param("providerId") String providerId);
    void saveSocialLogin(SocialLoginsVO socialLogin);
    void updateRefreshToken(SocialLoginsVO socialLogin);

    //Refresh Token으로 소셜 로그인 정보 조회
    SocialLoginsVO findSocialLoginByRefreshToken(String refreshToken);

    //특정 사용자의 Refresh Token 삭제 (로그아웃)
    void clearRefreshTokenByUserId(Long userId);

    //사용자 ID로 소셜 로그인 정보 조회 (토큰 갱신용)
    SocialLoginsVO findSocialLoginByUserId(Long userId);

}