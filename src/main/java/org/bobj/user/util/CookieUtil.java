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
    @Value("${server.domain:https://half-to-half.site/}")
    private String serverDomain;

    /**
     * 요청 출처가 localhost 계열인지 판별
     */
    private boolean isLocalhostRequest(String source) {
        if (source == null) return true;
        return source.contains("localhost") || source.contains("127.0.0.1") || source.contains("0.0.0.0");
    }

    /**
     * 요청 헤더를 기반으로 동적 쿠키 도메인 결정 (localhost면 null 반환)
     */
    private String determineCookieDomain(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        String host = request.getHeader("Host");

        String requestSource = origin != null ? origin : (referer != null ? referer : (host != null ? ("http://" + host) : null));

        if (requestSource == null) {
            log.debug("요청 소스 미확인, 기본적으로 localhost로 간주");
            return null;
        }

        if (isLocalhostRequest(requestSource)) {
            log.debug("🔍 Localhost 요청 감지 - 쿠키 도메인 설정 생략 (출처: {})", requestSource);
            return null;
        } else {
            String domain = extractDomainFromUrl(requestSource);
            log.debug("🔍 실제 도메인 요청 감지 - 쿠키 도메인: {} (출처: {})", domain, requestSource);
            return domain;
        }
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
     * 요청이 HTTPS인지 확인
     */
    private boolean isRequestSecure(HttpServletRequest request) {
        if (request == null) return false;
        if (request.isSecure()) return true;
        String requestURL = request.getRequestURL().toString();
        return requestURL.startsWith("https://");
    }

    /**
     * 쿠키 설정 (로컬/배포 자동 분기)
     *
     * 동작 원칙:
     * - 배포(실제 도메인) && HTTPS: SameSite=None; Secure=true
     * - 로컬(http): SameSite=Lax; Secure=false  (브라우저가 SameSite=None에 Secure를 강제하기 때문)
     */
    private void setCookieCommon(HttpServletResponse response, HttpServletRequest request,
                                 String name, String value, int maxAge) {
        String dynamicDomain = determineCookieDomain(request);
        boolean isLocalhost = (dynamicDomain == null);
        boolean isHttps = isRequestSecure(request);
        boolean useSecure = !isLocalhost && isHttps;

        String sameSite;
        if (isLocalhost) {
            sameSite = "SameSite=Lax";  // 로컬에서 cross-site는 불가능하지만, 기본 요청엔 쿠키 전달 가능
        } else {
            sameSite = "SameSite=None"; // 운영에서 cross-site 가능
        }

        String secureFlag = useSecure ? "; Secure" : "";
        String domainPart = dynamicDomain != null ? String.format("; Domain=%s", dynamicDomain) : "";

        String cookieHeader = String.format(
                "%s=%s; Path=/; HttpOnly%s; Max-Age=%d; %s%s",
                name, value, domainPart, maxAge, sameSite, secureFlag
        );

        response.addHeader("Set-Cookie", cookieHeader);
        log.debug("🍪 쿠키 설정: {}, 도메인: {}, HTTPS: {}, Secure: {}, {}, 만료: {}초",
                name, dynamicDomain != null ? dynamicDomain : "자동", isHttps, useSecure, sameSite, maxAge);
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
                    log.debug("🔍 쿠키 발견: {} = {}", cookieName, cookie.getValue());
                    return cookie.getValue();
                }
            }
        }
        log.debug("❌ 쿠키 없음: {}", cookieName);
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
        String dynamicDomain = determineCookieDomain(request);
        boolean isLocalhost = (dynamicDomain == null);
        boolean isHttps = isRequestSecure(request);
        boolean useSecure = !isLocalhost && isHttps;

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ZERO);

        if (isLocalhost) {
            builder.sameSite("Lax").secure(false);
        } else {
            builder.sameSite("None").secure(useSecure);
        }

        if (!isLocalhost && dynamicDomain != null) {
            builder.domain(dynamicDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
        log.info("🗑️ 쿠키 삭제: {} (domain={}, isLocalhost={}, secure={})", cookieName, dynamicDomain != null ? dynamicDomain : "자동/없음", isLocalhost, (isLocalhost ? false : useSecure));
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
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            log.info("🍪 요청에 포함된 쿠키들:");
            for (Cookie cookie : cookies) {
                log.info("  - {} = {}", cookie.getName(), cookie.getValue());
            }
        } else {
            log.warn("❌ 요청에 쿠키가 없습니다!");
        }
    }
}
