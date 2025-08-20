package org.bobj.config;

import org.bobj.user.service.CustomOAuth2UserService;
import org.bobj.user.security.JwtAuthenticationFilter;
import org.bobj.user.security.JwtTokenProvider;
import org.bobj.user.security.OAuth2LoginSuccessHandler;
import org.bobj.user.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    
    @Autowired
    private CookieUtil cookieUtil;

    @Value("${custom.oauth2.redirect-uri}")
    private String frontendRedirectUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // ================================
        // 🚀 개발용 설정 (API 테스트 편의성)
        // ================================
        http
                .cors().configurationSource(corsConfigurationSource())
                .and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests(authorize -> authorize
                        //개발 중 - 모든 API 접근 허용 (JWT 토큰 없어도 OK)
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler())
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService())
                        )
                );

        // JWT 필터는 유지 (토큰이 있으면 인증 처리, 없어도 통과)
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

        /* ================================
         * 운영용 보안 설정 (주석 처리)
         * 배포 시 위의 개발용 설정을 주석처리하고 아래 설정을 활성화하세요
         * ================================*/

//        http
//                .cors().configurationSource(corsConfigurationSource())
//                .and()
//                .csrf().disable()
//                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                .and()
//                .authorizeRequests(authorize -> authorize
//                        .antMatchers("/api/signup").hasAuthority("PRE_AUTH")
//
//                        // 인증 필요한 API (정규 토큰만 접근 가능)
//                        .antMatchers("/api/auth/**").authenticated()
//
//                        // 인증 불필요한 API (토큰 없어도 접근 가능)
//                        .antMatchers(
//                                "/api/**",                           // 일반 API
//                                "/", "/error",                       // 기본 경로
//                                "/test",                             // 테스트 경로
//                                "/login/oauth2/code/**",             // OAuth2 리다이렉트
//                                "/swagger-ui.html", "/swagger-resources/**",
//                                "/v2/api-docs", "/v3/api-docs/**", "/webjars/**",
//                                "/swagger-ui/**"                     // Swagger 문서
//                        ).permitAll()
//
//                        // 기타 모든 요청은 거부
//                        .anyRequest().authenticated()
//
//                )
//                // 인증 실패 시 JSON 응답 반환하도록 설정
//                .exceptionHandling(exceptions -> exceptions
//                        .authenticationEntryPoint((request, response, authException) -> {
//                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                            response.setContentType("application/json;charset=UTF-8");
//                            response.getWriter().write("{\"error\":\"Authentication required\",\"message\":\"Access token required for this endpoint\"}");
//                        })
//                        .accessDeniedHandler((request, response, accessDeniedException) -> {
//                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//                            response.setContentType("application/json;charset=UTF-8");
//                            response.getWriter().write("{\"error\":\"Access denied\",\"message\":\"Insufficient permissions\"}");
//                        })
//                )
//                .oauth2Login(oauth2 -> oauth2
//                        .successHandler(oAuth2LoginSuccessHandler())
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(customOAuth2UserService())
//                        )
//                );
//
//        // JWT 인증 필터 추가
//        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userService, cookieUtil);
    }

    @Bean
    public OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler() {
        // Spring Non-Boot 환경에서 값 직접 주입
        return new OAuth2LoginSuccessHandler(jwtTokenProvider, userService, cookieUtil, frontendRedirectUri);
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        return new CustomOAuth2UserService();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 개발 환경 도메인
        configuration.addAllowedOrigin("http://localhost:5173");    // Vue3 + Vite 기본 포트
        configuration.addAllowedOrigin("http://localhost:8080");    // Vue3 + Vue CLI 기본 포트
        configuration.addAllowedOrigin("http://localhost:8081");    // 기존 프론트엔드 포트

        configuration.addAllowedOrigin("https://localhost:5173"); //vite secutiry 포트

        // 배포 환경 도메인
        configuration.addAllowedOrigin("https://half-to-half.site");        // 백엔드 배포 URL
        configuration.addAllowedOrigin("https://half-to-half.vercel.app");  // 프론트엔드 배포 URL

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
        configuration.addAllowedOriginPattern("http://localhost:[*]");

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