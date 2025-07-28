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
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * OAuth2 클라이언트(카카오 등)의 등록 정보를 설정하는 클래스.
 */
@Configuration
@PropertySource("classpath:application.properties") // application.properties 파일의 내용을 읽어옵니다.
public class OAuth2ClientConfig {

    // @PropertySource로 읽어온 프로퍼티 값에 접근하기 위한 Environment 객체
    @Resource
    private Environment env;

    /**
     * OAuth2 클라이언트들의 정보를 담고 있는 저장소(Repository) Bean을 생성합니다
     * @return 구성된 ClientRegistrationRepository 객체.
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {

        // 환경변수에서 값 읽기 (null 안전성 보장)
        String clientId = Optional.ofNullable(System.getenv("KAKAO_CLIENT_ID"))
                .orElse("d6f410db14e162483d6845398daf3718");
        String clientSecret = Optional.ofNullable(System.getenv("KAKAO_CLIENT_SECRET"))
                .orElse("37dIrL5vdJT4uE4PrAjr7HrNc2LqKTgm");
        String redirectUri = Optional.ofNullable(System.getenv("OAUTH_REDIRECT_URI"))
                .orElse("http://localhost:8080/login/oauth2/code/kakao");

        // NPE 방지: 스코프 처리 안전성 보장
        String scopeString = Optional.ofNullable(System.getenv("OAUTH_SCOPE"))
                .orElse("profile_nickname,account_email");

        Set<String> scopes = new HashSet<>();
        if (scopeString != null && !scopeString.trim().isEmpty()) {
            String[] scopeArray = scopeString.trim().split(",");  // 이제 NPE 안전
            for (String scope : scopeArray) {
                if (scope != null && !scope.trim().isEmpty()) {
                    scopes.add(scope.trim());
                }
            }
        } else {
            scopes.add("profile_nickname");
            scopes.add("account_email");
        }

        ClientRegistration kakaoClientRegistration = ClientRegistration.withRegistrationId("kakao")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)  // redirectUriTemplate → redirectUri
                .scope(scopes)
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();

        return new InMemoryClientRegistrationRepository(kakaoClientRegistration);
    }
}