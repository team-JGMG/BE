package org.bobj.user.config;

//import org.apache.ibatis.mapping.Environment;
import org.springframework.core.env.Environment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * OAuth2 클라이언트(카카오 등)의 등록 정보를 설정하는 클래스.
 * non-Spring Boot 환경에서는 이처럼 수동으로 Bean을 등록해야 합니다.
 */
@Configuration
@PropertySource("classpath:application.properties") // application.properties 파일의 내용을 읽어옵니다.
public class OAuth2ClientConfig {

    // @PropertySource로 읽어온 프로퍼티 값에 접근하기 위한 Environment 객체
    @Resource
    private Environment env;

    /**
     * OAuth2 클라이언트들의 정보를 담고 있는 저장소(Repository) Bean을 생성합니다.
     * Spring Security는 이 Bean을 참조하여 사용할 OAuth2 클라이언트 정보를 찾습니다.
     *
     * @return 구성된 ClientRegistrationRepository 객체.
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {

        // 1. 프로퍼티 파일에서 값들을 읽어옵니다.
        String scope = env.getProperty("spring.security.oauth2.client.registration.kakao.scope");
        String clientName = env.getProperty("spring.security.oauth2.client.registration.kakao.client-name");
        String grantType = env.getProperty("spring.security.oauth2.client.registration.kakao.authorization-grant-type");

        // 2. ClientRegistration 빌더를 사용하여 클라이언트 정보를 설정합니다.
        ClientRegistration kakaoClientRegistration = ClientRegistration.withRegistrationId("kakao")
                .clientId(env.getProperty("spring.security.oauth2.client.registration.kakao.client-id"))
                .clientSecret(env.getProperty("spring.security.oauth2.client.registration.kakao.client-secret"))
                // 3. 읽어온 값들을 사용하여 설정합니다.
                .scope(scope.split(",")) // 쉼표로 구분된 문자열을 배열로 변환
                .authorizationGrantType(new AuthorizationGrantType(grantType))
                .clientName(clientName)
                .clientAuthenticationMethod(ClientAuthenticationMethod.POST)
                .redirectUriTemplate(env.getProperty("spring.security.oauth2.client.registration.kakao.redirect-uri"))
                .authorizationUri(env.getProperty("spring.security.oauth2.client.provider.kakao.authorization-uri"))
                .tokenUri(env.getProperty("spring.security.oauth2.client.provider.kakao.token-uri"))
                .userInfoUri(env.getProperty("spring.security.oauth2.client.provider.kakao.user-info-uri"))
                .userNameAttributeName(env.getProperty("spring.security.oauth2.client.provider.kakao.user-name-attribute"))
                .build();

        return new InMemoryClientRegistrationRepository(Collections.singletonList(kakaoClientRegistration));
    }
}