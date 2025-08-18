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

    @Value("${jwt.access-token-expire-minutes:30}")
    private int accessTokenExpireMinutes;

    @Value("${jwt.refresh-token-expire-days:7}")
    private int refreshTokenExpireDays;

    @Value("${jwt.pre-auth-token-expire-minutes:10}")
    private int preAuthTokenExpireMinutes;

    @Value("${jwt.preemptive-refresh-minutes:5}")
    private int preemptiveRefreshMinutes;

    @Value("${jwt.preemptive-refresh-enabled:true}")
    private boolean preemptiveRefreshEnabled;

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
        long expireTime = preAuthTokenExpireMinutes * 60 * 1000L; // 설정값 사용
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireTime))
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
        long expireTime = accessTokenExpireMinutes * 60 * 1000L; // 설정값 사용
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * @param email 이메일 (subject)
     * @return JWT Refresh Token 문자열
     */
    public String createRefreshToken(String email) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("type", "refresh");

        Date now = new Date();
        long expireTime = refreshTokenExpireDays * 24 * 60 * 60 * 1000L; // 설정값 사용
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireTime))
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
     * signup에선 pre-token이 우선됨.
     */
    public String resolveToken(HttpServletRequest request) {
        String path = request.getRequestURI();
        log.debug("[토큰 추출 시작] 요청 URI: {}", path);

        // 1. Authorization Header에서 토큰 추출
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            log.debug("[토큰 발견] Authorization Header에서 토큰 추출");
            return bearerToken.substring(7);
        }
        log.debug("[토큰 없음] Authorization Header에 토큰 없음");

        // 2. 쿠키에서 토큰 추출
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            log.debug("[쿠키 확인] 총 {}개 쿠키 발견", cookies.length);

            if ("/api/auth/signup".equals(path)) {
                // 회원가입 시에는 preAuthToken 우선 사용
                for (Cookie cookie : cookies) {
                    if ("preAuthToken".equals(cookie.getName())) {
                        log.debug("[토큰 발견] preAuthToken 쿠키에서 토큰 추출 (회원가입용)");
                        return cookie.getValue();
                    }
                }
                log.debug("[preAuthToken 없음] 회원가입 요청에 preAuthToken 쿠키 없음");
                return null; // 회원가입 시 preAuthToken 없으면 인증 실패 처리 가능
            } else {
                // 회원가입 외 경로는 기존 순서 유지 (accessToken 우선)
                for (Cookie cookie : cookies) {
                    if ("accessToken".equals(cookie.getName())) {
                        log.debug("[토큰 발견] accessToken 쿠키에서 토큰 추출");
                        return cookie.getValue();
                    }
                }
                for (Cookie cookie : cookies) {
                    if ("preAuthToken".equals(cookie.getName())) {
                        log.debug("[토큰 발견] preAuthToken 쿠키에서 토큰 추출");
                        return cookie.getValue();
                    }
                }
            }
        } else {
            log.debug("[쿠키 없음] 요청에 쿠키가 전혀 없음");
        }

        log.debug("[토큰 추출 실패] Authorization Header와 쿠키 모두에서 토큰을 찾을 수 없음");
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
     * 토큰이 곧 만료될 예정인지 확인 (사전 갱신용)
     * @param token 확인할 토큰
     * @param minutesBeforeExpiry 만료 몇 분 전인지 (기본: 설정값 사용)
     * @return true: 사전 갱신 필요, false: 아직 충분한 시간 남음
     */
    public boolean isTokenNearExpiry(String token, int minutesBeforeExpiry) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            Date now = new Date();

            long timeUntilExpiry = expiration.getTime() - now.getTime();
            long thresholdTime = minutesBeforeExpiry * 60 * 1000L; // 분 → 밀리초 변환

            // 만료까지 남은 시간이 임계값보다 적으면 true
            boolean nearExpiry = timeUntilExpiry > 0 && timeUntilExpiry < thresholdTime;

            if (nearExpiry) {
                log.debug("토큰 만료 임박 감지: {}분 후 만료 예정", timeUntilExpiry / (60 * 1000));
            }

            return nearExpiry;

        } catch (ExpiredJwtException e) {
            // 이미 만료된 토큰 - 사전 갱신 대상 아님 (기존 만료 갱신 로직에서 처리)
            return false;
        } catch (Exception e) {
            log.warn("토큰 만료 임박 체크 중 오류: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰이 곧 만료될 예정인지 확인 (설정값 기본 사용)
     */
    public boolean isTokenNearExpiry(String token) {
        return isTokenNearExpiry(token, preemptiveRefreshMinutes); // 설정값 사용
    }

    /**
     * 사전 갱신이 활성화되어 있고 토큰이 곧 만료될 예정인지 확인
     */
    public boolean shouldPreemptivelyRefresh(String token) {
        return preemptiveRefreshEnabled && isTokenNearExpiry(token);
    }

    /**
     * 토큰의 남은 만료 시간을 분 단위로 반환
     */
    public long getTokenRemainingMinutes(String token) {
        try {
            Claims claims = getClaims(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();

            long timeUntilExpiry = expiration.getTime() - now.getTime();
            return Math.max(0, timeUntilExpiry / (60 * 1000L)); // 밀리초 → 분 변환

        } catch (Exception e) {
            log.warn("토큰 만료 시간 계산 중 오류: {}", e.getMessage());
            return 0;
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
