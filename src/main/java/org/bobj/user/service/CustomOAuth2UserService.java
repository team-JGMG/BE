package org.bobj.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    // 데이터베이스(UserMapper) 의존성을 완전히 제거합니다.

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("CustomOAuth2UserService: DB 저장 없이 소셜 정보 처리 시작");
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 카카오에서 받은 원본 속성들
        Map<String, Object> originalAttributes = oAuth2User.getAttributes();

        // SuccessHandler에서 사용할 수 있도록 새로운 속성 맵을 생성합니다.
        Map<String, Object> customAttributes = new HashMap<>(originalAttributes);
        customAttributes.put("provider", registrationId);

        Map<String, Object> kakaoAccount = (Map<String, Object>) originalAttributes.get("kakao_account");
        String email = (String) kakaoAccount.get("email");

        if (email == null) {
            throw new OAuth2AuthenticationException("카카오 계정에서 이메일 정보를 가져올 수 없습니다. 동의 항목을 확인하세요.");
        }

        log.info("소셜 정보 확인 완료: email={}, provider={}", email, registrationId);

        // DB를 거치지 않으므로, '사전 인증' 상태를 나타내는 임시 권한을 부여합니다.
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_PRE_AUTH")),
                customAttributes,
                userNameAttributeName);
    }
}