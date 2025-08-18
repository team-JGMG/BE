package org.bobj.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * OAuth2 클라이언트(카카오)의 순수 수동 설정 클래스
 * Spring Boot Auto Configuration과의 충돌을 방지하기 위해 커스텀 프로퍼티 사용
 */
@Slf4j
@Configuration
public class OAuth2ClientConfig {

    // 커스텀 OAuth2 프로퍼티 사용 (Spring Boot Auto Configuration 트리거 방지)
    @Value("${oauth2.kakao.client-id}")
    private String clientId;

    @Value("${oauth2.kakao.client-secret}")
    private String clientSecret;

    @Value("${oauth2.kakao.redirect-uri}")
    private String redirectUri;

    @Value("${oauth2.kakao.scope}")
    private String scopeString;

    /**
     * OAuth2 클라이언트 등록 정보를 담고 있는 저장소 Bean 생성
     * 순수 수동 설정으로 Spring Boot Auto Configuration 비활성화
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        
        log.info("순수 수동 OAuth2 클라이언트 설정 초기화 시작");
        log.info("카카오 클라이언트 ID: {}...",
                clientId != null && clientId.length() > 8 ? 
                clientId.substring(0, 8) + "****" : "설정되지 않음");
        log.info("카카오 리다이렉트 URI: {}", redirectUri);
        log.info("요청 스코프: {}", scopeString);

        // 스코프 파싱
        Set<String> scopes = parseScopes(scopeString);
        log.info("파싱된 스코프: {}", scopes);

        // 카카오 클라이언트 등록 정보 생성 (순수 수동 설정)
        ClientRegistration kakaoClientRegistration = ClientRegistration.withRegistrationId("kakao")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope(scopes)
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();

        log.info("OAuth2 클라이언트 설정 완료");
        return new InMemoryClientRegistrationRepository(kakaoClientRegistration);
    }

    /**
     * 스코프 문자열을 파싱하여 Set으로 변환
     */
    private Set<String> parseScopes(String scopeString) {
        Set<String> scopes = new HashSet<>();
        
        if (scopeString != null && !scopeString.trim().isEmpty()) {
            Arrays.stream(scopeString.split(","))
                    .map(String::trim)
                    .filter(scope -> !scope.isEmpty())
                    .forEach(scopes::add);
        }
        
        // 기본 스코프 보장
        if (scopes.isEmpty()) {
            scopes.add("profile_nickname");
            scopes.add("account_email");
            log.warn("스코프가 비어있어 기본 스코프를 사용합니다: {}", scopes);
        }
        
        return scopes;
    }
}
