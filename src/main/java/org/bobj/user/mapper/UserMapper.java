package org.bobj.user.mapper;

import org.bobj.user.domain.SocialLoginsVO;
import org.bobj.user.domain.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Optional;

@Mapper
public interface UserMapper {

    // USERS 테이블 관련
    Optional<UserVO> findUserByEmail(String email);
    Optional<UserVO> findUserById(long userId);
    void saveUser(UserVO user);
    void updateUser(UserVO user);

    // SOCIAL_LOGINS 테이블 관련
    Optional<SocialLoginsVO> findSocialLoginByProviderAndProviderId(@Param("provider") String provider, @Param("providerId") String providerId);
    void saveSocialLogin(SocialLoginsVO socialLogin);
    void updateRefreshToken(SocialLoginsVO socialLogin);
}