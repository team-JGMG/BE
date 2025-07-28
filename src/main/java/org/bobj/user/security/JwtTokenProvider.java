package org.bobj.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
//@ControllerAdvice
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final long PRE_AUTH_TOKEN_EXPIRE_TIME = 10 * 60 * 1000L;      // 10분
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 30 * 60 * 1000L;      // 30분
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L; // 7일

    private Key key;
    private final UserDetailsService userDetailsService;

    @PostConstruct
    protected void init() {
        key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /*** DB 저장 전, 소셜 로그인 정보만을 담는 임시 토큰을 생성합니다.
     */
    public String createPreAuthToken(String email, String nickname, String provider, String providerId) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("nickname", nickname);
        claims.put("provider", provider);
        claims.put("providerId", providerId);
        claims.put("type", "pre-auth"); // 토큰 타입 명시

        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + PRE_AUTH_TOKEN_EXPIRE_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * @param email 이메일 (subject)
     * @param userId 사용자 ID
     * @param isAdmin 관리자 여부 (true: ADMIN, false: USER)
     * @return JWT Access Token 문자열
     */
    public String createAccessToken(String email, Long userId, boolean isAdmin) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("type", "access");
        claims.put("userId", userId);  // userId 추가
        claims.put("role", isAdmin ? "ADMIN" : "USER");

        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 이전 버전 호환용 (deprecated)
     */
    @Deprecated
    public String createAccessToken(String email, boolean isAdmin) {
        return createAccessToken(email, null, isAdmin);
    }

    /**
     * @param email 이메일 (subject)
     * @return JWT Refresh Token 문자열
     */
    public String createRefreshToken(String email) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("type", "refresh");

        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRE_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Access Token으로 Spring Security 인증 객체 생성
     * (DB 조회 포함)
     */
    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUserPk(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * 토큰에서 모든 클레임(정보) 추출 (사전 인증 토큰 정보 등)
     */
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰에서 subject(email) 추출
     */
    public String getUserPk(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 토큰에서 userId 추출
     */
    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj == null) {
            return null;
        }
        // Integer나 Long으로 저장될 수 있으므로 안전하게 변환
        return userIdObj instanceof Integer ? 
            ((Integer) userIdObj).longValue() : 
            (Long) userIdObj;
    }

    /**
     * HTTP 요청 헤더에서 토큰 값 추출 ("Authorization: Bearer [TOKEN]")
     */
    /**
     * HTTP 요청에서 토큰 값 추출
     * 1순위: Authorization Header ("Authorization: Bearer [TOKEN]")
     * 2순위: Cookie에서 accessToken 추출 (일반 인증용)
     * 3순위: Cookie에서 preAuthToken 추출 (사전 인증용)
     */
    public String resolveToken(HttpServletRequest request) {
        // 1. Authorization Header에서 토큰 추출
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. Cookie에서 토큰 추출
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                // Access Token (일반 인증용)
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
                // Pre-Auth Token (사전 인증용)
                if ("preAuthToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    /**
     * 토큰 유효성 및 만료시간 검증
     */
    public boolean validateToken(String jwtToken) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwtToken);
            return !claims.getBody().getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 만료된 토큰에서 사용자 이메일 추출
     * 자동 토큰 갱신을 위해 사용
     */
    public String getUserEmailFromExpiredToken(String expiredToken) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(expiredToken)
                    .getBody();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰의 Claims 추출
            return e.getClaims().getSubject();
        } catch (Exception e) {
            log.warn("만료된 토큰에서 이메일 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰이 만료되었는지만 확인 (서명은 유효한지 체크)
     * 자동 토큰 갱신을 위해 사용
     */
    public boolean isTokenExpired(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return false; // 유효한 토큰
        } catch (ExpiredJwtException e) {
            return true; // 만료된 토큰 (서명은 유효)
        } catch (Exception e) {
            return false; // 잘못된 토큰
        }
    }

    /**
     * 토큰에서 사용자 역할 추출
     */
    public String getUserRole(String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }
}
