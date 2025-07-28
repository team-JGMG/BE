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
                        // 인증 불필요한 API들 (가장 먼저 적용)
                        .antMatchers(
                                "/", "/error",
                                "/test",                               // 테스트 API
                                "/api/auth/login/**",// 로그인 시작 관련 (kakao)
                                "/api/auth/signup",
                                "/login/oauth2/code/**",              // 카카오 리다이렉트
                                "/swagger-ui.html", "/swagger-resources/**",
                                "/v2/api-docs", "/webjars/**"
                        ).permitAll()

                        // 관리자 전용 API
                        //.antMatchers("/api/admin/**").hasRole("ADMIN")
                        
                        // 사용자 정보 관련 (권한별 구분)
                        .antMatchers("/api/users/me").hasAnyRole("USER", "ADMIN")           // 본인 정보는 모든 인증 사용자
                        .antMatchers("/api/users/**").hasRole("ADMIN")                      // 타인 정보는 관리자만
                        
                        // 인증 필요 API들
                        .antMatchers(
                                "/api/auth/oauth/**"              // 로그아웃
                        ).authenticated()

                        // 그 외 모든 요청은 인증 필요
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

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.addAllowedOrigin("http://localhost:8081");
//        configuration.addAllowedMethod("*");
//        configuration.addAllowedHeader("*");
//        configuration.setAllowCredentials(true);
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Vue3 개발 환경 도메인 설정
        configuration.addAllowedOrigin("http://localhost:3000");    // Vue3 + Vite 대안 포트
        configuration.addAllowedOrigin("http://localhost:5173");    // Vue3 + Vite 기본 포트
        configuration.addAllowedOrigin("http://localhost:8080");    // Vue3 + Vue CLI 기본 포트
        configuration.addAllowedOrigin("http://localhost:8081");    // 현재 프론트엔드 포트 (기존 설정)

        // HTTP 메서드 허용 설정
        configuration.addAllowedMethod("GET");       // 데이터 조회 (로그인 시작, 콜백 등)
        configuration.addAllowedMethod("POST");      // 데이터 생성/전송 (회원가입, 로그아웃 등)
        configuration.addAllowedMethod("PUT");       // 데이터 수정 (프로필 업데이트 등)
        configuration.addAllowedMethod("DELETE");    // 데이터 삭제 (계정 삭제 등)
        configuration.addAllowedMethod("PATCH");     // 부분 수정 (상태 변경 등)
        configuration.addAllowedMethod("OPTIONS");   // Preflight 요청 (브라우저 자동 요청)

        // HTTP 헤더 허용 설정
        configuration.addAllowedHeader("Content-Type");     // JSON/Form 데이터 타입 지정
        configuration.addAllowedHeader("Authorization");    // JWT 토큰 인증 헤더
        configuration.addAllowedHeader("X-Requested-With"); // Ajax 요청 식별
        configuration.addAllowedHeader("Accept");           // 응답 데이터 타입 지정
        configuration.addAllowedHeader("Origin");           // 요청 출처 도메인
        configuration.addAllowedHeader("Access-Control-Request-Method");  // Preflight 메서드
        configuration.addAllowedHeader("Access-Control-Request-Headers"); // Preflight 헤더

        // 쿠키/인증 정보 허용 설정
        configuration.setAllowCredentials(true);    // HttpOnly 쿠키, Authorization 헤더 포함 요청 허용
        // Vue3에서 axios.defaults.withCredentials = true 시 필요

        // 브라우저 캐시 최적화 설정
        configuration.setMaxAge(3600L);            // Preflight 요청 결과를 1시간(3600초) 캐시
        // Vue3 개발 중 OPTIONS 요청 최소화로 성능 향상

        // 클라이언트 접근 가능 헤더 설정
        configuration.addExposedHeader("Authorization");   // Vue3에서 응답의 Authorization 헤더 읽기 허용
        configuration.addExposedHeader("Set-Cookie");      // 쿠키 설정 헤더 노출 (디버깅용)
        configuration.addExposedHeader("Access-Control-Allow-Origin");     // CORS 정보 노출
        configuration.addExposedHeader("Access-Control-Allow-Credentials"); // 인증 정보 노출

        // 모든 API 경로에 CORS 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // 모든 경로 (/, /api/*, /oauth2/* 등)

        return source;
    }

}