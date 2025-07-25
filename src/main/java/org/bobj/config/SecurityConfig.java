package org.bobj.config;

import org.bobj.user.service.CustomOAuth2UserService;
import org.bobj.user.security.JwtAuthenticationFilter;
import org.bobj.user.security.JwtTokenProvider;
import org.bobj.user.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private org.bobj.user.service.UserService userService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors().configurationSource(corsConfigurationSource())
                .and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests(authorize -> authorize
                        // 인증이 필요한 API들을 먼저 명시 (더 구체적인 패턴)
                        .antMatchers(
                                "/api/auth/logout",                   // 로그아웃
                                "/api/auth/token/refresh",            // 토큰 갱신
                                "/api/users/**"                       // 모든 사용자 관련 API
                        ).authenticated()

                        // 인증 불필요한 API들 (덜 구체적인 패턴을 뒤에)
                        .antMatchers(
                                "/", "/error",
                                "/test",                               // 테스트 API
                                "/api/auth/oauth/**",                 // OAuth 관련 (kakao-url, callback)
                                "/api/auth/signup/complete",          // 회원가입 완료
                                "/login/oauth2/code/**",              // 카카오 리다이렉트
                                "/oauth2/**","/test-tokens/**",                         // OAuth2 관련 경로
                                "/swagger-ui.html", "/swagger-resources/**",
                                "/v2/api-docs", "/webjars/**"
                        ).permitAll()

                        // 그 외 모든 요청도 인증 필요
                        .anyRequest().authenticated()
                )
                // 인증 실패 시 JSON 응답 반환하도록 설정
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"Access denied\"}");
                        })
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler())
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService())
                        )
                );

        // JWT 인증 필터 추가
        http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler() {
        return new OAuth2LoginSuccessHandler(jwtTokenProvider);
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        return new CustomOAuth2UserService();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:8081");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}