package org.bobj.user.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // 1. /api/auth/register-final 경로는 인증 필터 스킵
        if (path.equals("/api/auth/register-final") || path.startsWith("/oauth2/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 토큰 추출
        String token = jwtTokenProvider.resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰 타입이 pre-auth 인지 확인
            String tokenType = jwtTokenProvider.getClaims(token).get("type", String.class);

            if (!"pre-auth".equals(tokenType)) {
                // 일반 access 토큰이라면 인증 세팅
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // pre-auth 토큰이면 인증 컨텍스트 세팅 없이 통과
        }

        filterChain.doFilter(request, response);
    }
}
