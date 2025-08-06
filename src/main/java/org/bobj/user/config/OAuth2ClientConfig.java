package org.bobj.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * OAuth2 클라이언트(카카오 등)의 등록 정보를 설정하는 클래스.
 */
@Slf4j
@Configuration
@PropertySource("classpath:application.properties")
public class OAuth2ClientConfig {

    // Spring의 @Value를 사용해서 application.properties 또는 환경변수에서 값 읽기
    @Value("${spring.security.oauth2.client.registration.kakao.client-id:d6f410db14e162483d6845398daf3718}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret:37dIrL5vdJT4uE4PrAjr7HrNc2LqKTgm}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri:http://localhost:8080/login/oauth2/code/kakao}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.registration.kakao.scope:profile_nickname,account_email}")
    private String scopeString;

    /**
     * OAuth2 클라이언트들의 정보를 담고 있는 저장소(Repository) Bean을 생성합니다
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        
        log.info("OAuth2 클라이언트 설정 초기화 시작");
        log.info("카카오 클라이언트 ID: {}...",
                clientId != null && clientId.length() > 8 ? 
                clientId.substring(0, 8) + "****" : "설정되지 않음");
        log.info("리다이렉트 URI: {}", redirectUri);
        log.info("요청 스코프: {}", scopeString);

        // 스코프 파싱
        Set<String> scopes = parseScopes(scopeString);
        log.info(" 파싱된 스코프: {}", scopes);

        // 카카오 클라이언트 등록 정보 생성
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
