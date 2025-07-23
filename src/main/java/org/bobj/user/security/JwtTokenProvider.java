package org.bobj.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@ControllerAdvice
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    // 토큰 유효시간 설정
    private static final long PRE_AUTH_TOKEN_EXPIRE_TIME = 10 * 60 * 1000L;      // 10분 (사전 인증 임시 토큰)
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
        claims.put("type", "pre-auth"); // 토큰 타입을 명시

        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + PRE_AUTH_TOKEN_EXPIRE_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 최종 회원가입 완료 후, 사용자의 역할(Role) 정보를 포함하여 생성합니다.
     */
    public String createAccessToken(String email) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("type", "access");

        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Refresh Token 생성 메소드
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
     * 최종 Access Token으로 Spring Security 인증 정보(Authentication)를 생성합니다.
     * (이 메소드는 DB 조회를 포함합니다)
     */
    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUserPk(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * [신규] 토큰에서 모든 클레임(정보)을 추출합니다.
     * 사전 인증 토큰의 정보를 읽을 때 사용됩니다.
     */
    public Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    /**
     * 토큰에서 Subject(email)를 추출합니다.
     */
    public String getUserPk(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Request의 Header에서 토큰 값을 가져옵니다. "Authorization" : "Bearer [TOKEN]"
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 토큰의 유효성 + 만료일자를 확인합니다.
     */
    public boolean validateToken(String jwtToken) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwtToken);
            return !claims.getBody().getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

}
