package org.bobj.user.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

@Slf4j
@Component
public class CookieUtil {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String PRE_AUTH_TOKEN_COOKIE_NAME = "preAuthToken";
    private static final int ACCESS_TOKEN_MAX_AGE = 30 * 60; // 30분
    private static final int PRE_AUTH_TOKEN_MAX_AGE = 15 * 60; // 15분

    // 배포 도메인 확인용(옵션)
    @Value("${server.domain:https://half-to-half.site}")
    private String serverDomain;

    /**
     * 백엔드 도메인 추출 (서버가 실행되는 도메인)
     */
    private String getBackendDomain() {
        try {
            if (serverDomain.startsWith("http://") || serverDomain.startsWith("https://")) {
                String domain = serverDomain.split("://")[1].split("/")[0].split(":")[0];
                String[] parts = domain.split("\\.");
                if (parts.length >= 2) {
                    return parts[parts.length - 2] + "." + parts[parts.length - 1];
                }
                return domain;
            }
            return "half-to-half.site"; // 기본값
        } catch (Exception e) {
            log.warn("백엔드 도메인 추출 실패: {}", serverDomain, e);
            return "half-to-half.site"; // 기본값
        }
    }

    /**
     * 쿠키 도메인 결정 (Cross-Origin 고려)
     */
    private String determineCookieDomain(HttpServletRequest request) {
        String host = request.getHeader("Host");
        String requestURL = request.getRequestURL().toString();
        
        // 로컬 환경 확인
        if (host != null && (host.contains("localhost") || host.contains("127.0.0.1"))) {
            log.debug("Localhost 환경 감지 - 쿠키 도메인 설정 생략: {}", host);
            return null;
        }
        
        if (requestURL != null && requestURL.contains("localhost")) {
            log.debug("Localhost URL 감지 - 쿠키 도메인 설정 생략: {}", requestURL);
            return null;
        }
        
        // 배포 환경에서는 항상 백엔드 도메인 사용
        String backendDomain = getBackendDomain();
        log.debug("배포 환경 감지 - 백엔드 도메인으로 쿠키 설정: {}", backendDomain);
        return backendDomain;
    }

    /**
     * URL에서 도메인 추출
     */
    private String extractDomainFromUrl(String url) {
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                String domain = url.split("://")[1].split("/")[0].split(":")[0];
                String[] parts = domain.split("\\.");
                if (parts.length >= 2) {
                    return parts[parts.length - 2] + "." + parts[parts.length - 1];
                }
                return domain;
            }
            return null;
        } catch (Exception e) {
            log.warn("도메인 추출 실패: {}", url, e);
            return null;
        }
    }

    /**
     * 요청이 HTTPS인지 확인 (Vercel/클라우드 프록시 고려)
     */
    private boolean isRequestSecure(HttpServletRequest request) {
        if (request == null) return false;
        
        // 1. 표준 HTTPS 확인
        if (request.isSecure()) return true;
        
        // 2. URL 확인
        String requestURL = request.getRequestURL().toString();
        if (requestURL.startsWith("https://")) return true;
        
        // 3. 프록시 헤더 확인 (Vercel, CloudFlare 등)
        String xForwardedProto = request.getHeader("X-Forwarded-Proto");
        if ("https".equalsIgnoreCase(xForwardedProto)) {
            log.debug("프록시를 통한 HTTPS 감지: X-Forwarded-Proto=https");
            return true;
        }
        
        // 4. Vercel 특화 헤더 확인
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String origin = request.getHeader("Origin");
        if (origin != null && origin.startsWith("https://")) {
            log.debug("Origin 헤더를 통한 HTTPS 감지: {}", origin);
            return true;
        }
        
        return false;
    }

    /**
     * Cross-Origin 요청인지 확인
     */
    private boolean isCrossOriginRequest(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String host = request.getHeader("Host");
        
        if (origin == null || host == null) return false;
        
        // Origin과 Host가 다르면 Cross-Origin
        return !origin.contains(host);
    }

    /**
     * 쿠키 설정 (Cross-Origin 지원)
     *
     * 동작 원칙:
     * - 로컬 환경: SameSite=Lax, Secure=false, Domain=null
     * - 배포 환경: SameSite=None, Secure=true, Domain=백엔드도메인
     */
    private void setCookieCommon(HttpServletResponse response, HttpServletRequest request,
                                 String name, String value, int maxAge) {
        String cookieDomain = determineCookieDomain(request);
        boolean isLocalhost = (cookieDomain == null);
        boolean isHttps = isRequestSecure(request);
        boolean isCrossOrigin = isCrossOriginRequest(request);
        
        // 로컬 환경과 배포 환경 구분
        String sameSite;
        boolean useSecure;
        
        if (isLocalhost) {
            // 로컬 개발 환경
            sameSite = "SameSite=Lax";
            useSecure = false;
        } else {
            // 배포 환경 (Cross-Origin 지원)
            sameSite = "SameSite=None";
            useSecure = true; // HTTPS 배포 환경에서는 항상 Secure
        }

        String secureFlag = useSecure ? "; Secure" : "";
        String domainPart = cookieDomain != null ? String.format("; Domain=%s", cookieDomain) : "";

        String cookieHeader = String.format(
                "%s=%s; Path=/; HttpOnly%s; Max-Age=%d; %s%s",
                name, value, domainPart, maxAge, sameSite, secureFlag
        );

        response.addHeader("Set-Cookie", cookieHeader);
        log.info("쿠키 설정: {}, 도메인: {}, HTTPS: {}, Cross-Origin: {}, Secure: {}, {}, 만료: {}초",
                name, cookieDomain != null ? cookieDomain : "자동(localhost)", isHttps, isCrossOrigin, useSecure, sameSite, maxAge);
    }

    /* 공개된 쿠키 설정 메소드들 */
    public void setAccessTokenCookie(HttpServletResponse response, HttpServletRequest request, String accessToken) {
        setCookieCommon(response, request, ACCESS_TOKEN_COOKIE_NAME, accessToken, ACCESS_TOKEN_MAX_AGE);
    }

    public void setPreAuthTokenCookie(HttpServletResponse response, HttpServletRequest request, String preAuthToken) {
        setCookieCommon(response, request, PRE_AUTH_TOKEN_COOKIE_NAME, preAuthToken, PRE_AUTH_TOKEN_MAX_AGE);
    }

    /* 쿠키 읽기 */
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request == null) return null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    log.debug("쿠키 발견: {} = {}", cookieName, cookie.getValue());
                    return cookie.getValue();
                }
            }
        }
        log.debug("쿠키 없음: {}", cookieName);
        return null;
    }

    public String getAccessTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
    }

    public String getPreAuthTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, PRE_AUTH_TOKEN_COOKIE_NAME);
    }

    /* 쿠키 삭제 */
    private void deleteCookieCommon(HttpServletResponse response, HttpServletRequest request, String cookieName) {
        String cookieDomain = determineCookieDomain(request);
        boolean isLocalhost = (cookieDomain == null);
        boolean isHttps = isRequestSecure(request);

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ZERO);

        if (isLocalhost) {
            // 로컬 환경
            builder.sameSite("Lax").secure(false);
        } else {
            // 배포 환경 (Cross-Origin 지원)
            builder.sameSite("None").secure(true);
            if (cookieDomain != null) {
                builder.domain(cookieDomain);
            }
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
        log.info("🗑️ 쿠키 삭제: {} (domain={}, isLocalhost={}, secure={})", 
                cookieName, cookieDomain != null ? cookieDomain : "자동(localhost)", isLocalhost, !isLocalhost);
    }

    public void deleteAccessTokenCookie(HttpServletResponse response, HttpServletRequest request) {
        deleteCookieCommon(response, request, ACCESS_TOKEN_COOKIE_NAME);
    }

    public void deletePreAuthTokenCookie(HttpServletResponse response, HttpServletRequest request) {
        deleteCookieCommon(response, request, PRE_AUTH_TOKEN_COOKIE_NAME);
    }

    public void logAllCookies(HttpServletRequest request) {
        if (request == null) {
            log.warn("요청이 null입니다.");
            return;
        }
        
        // 요청 헤더 정보 로깅
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        String host = request.getHeader("Host");
        String xForwardedProto = request.getHeader("X-Forwarded-Proto");
        
        log.info("요청 헤더 정보:");
        log.info("  - Origin: {}", origin);
        log.info("  - Host: {}", host);
        log.info("  - Referer: {}", referer);
        log.info("  - X-Forwarded-Proto: {}", xForwardedProto);
        log.info("  - Request URL: {}", request.getRequestURL());
        log.info("  - Is Secure: {}", request.isSecure());
        log.info("  - Is HTTPS (판정): {}", isRequestSecure(request));
        log.info("  - Is Cross-Origin: {}", isCrossOriginRequest(request));
        log.info("  - 백엔드 도메인: {}", getBackendDomain());
        
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            log.info("요청에 포함된 쿠키들:");
            for (Cookie cookie : cookies) {
                log.info("  - {} = {}", cookie.getName(), cookie.getValue());
            }
        } else {
            log.warn("요청에 쿠키가 없습니다!");
        }
    }
}
