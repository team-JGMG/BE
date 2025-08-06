package org.bobj.user.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class CookieUtil {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String PRE_AUTH_TOKEN_COOKIE_NAME = "preAuthToken";
    private static final int ACCESS_TOKEN_MAX_AGE = 30 * 60; // 30분
    private static final int PRE_AUTH_TOKEN_MAX_AGE = 15 * 60; // 15분

    @Value("${server.domain:https://half-to-half.site/}")
    private String serverDomain;

    /**
     * 요청 헤더를 기반으로 완전 동적 쿠키 도메인 결정
     * - localhost 계열 요청 → 도메인 설정 안함 (브라우저가 자동 처리)
     * - 실제 도메인 요청 → 해당 도메인을 쿠키 도메인으로 설정
     */
    private String determineCookieDomain(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        String host = request.getHeader("Host");
        
        // 요청 출처 분석
        String requestSource = origin != null ? origin : (referer != null ? referer : ("http://" + host));
        
        // localhost 계열 감지 (localhost, 127.0.0.1, 포트 포함)
        boolean isLocalhost = requestSource.contains("localhost") || 
                             requestSource.contains("127.0.0.1") ||
                             requestSource.contains("0.0.0.0");
        
        if (isLocalhost) {
            log.debug("🔍 Localhost 요청 감지 - 쿠키 도메인 설정 생략 (출처: {})", requestSource);
            return null; // 도메인 설정하지 않음
        } else {
            // 실제 도메인에서 도메인명 추출
            String domain = extractDomainFromUrl(requestSource);
            log.debug("🔍 실제 도메인 요청 감지 - 쿠키 도메인: {} (출처: {})", domain, requestSource);
            return domain;
        }
    }
    
    /**
     * URL에서 도메인명 추출
     * https://api.half-to-half.site → half-to-half.site
     * https://half-to-half.site → half-to-half.site
     */
    private String extractDomainFromUrl(String url) {
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                String domain = url.split("://")[1].split("/")[0].split(":")[0];
                
                // 서브도메인이 있는 경우 메인 도메인으로 변경 (쿠키 공유를 위해)
                String[] parts = domain.split("\\.");
                if (parts.length >= 2) {
                    // 마지막 두 부분만 사용 (예: api.half-to-half.site → half-to-half.site)
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
     * 공통 쿠키 설정 메소드 (동적 도메인, HTTPS 감지)
     */
    private void setCookieCommon(HttpServletResponse response, HttpServletRequest request, String name, String value, int maxAge) {
        // 🔥 요청 Origin에 따른 동적 도메인 결정
        String dynamicDomain = determineCookieDomain(request);
        
        // HTTPS 환경 감지하여 Secure 플래그 자동 설정
        boolean isHttps = serverDomain.startsWith("https://");
        String secureFlag = isHttps ? "; Secure" : "";
        
        // 도메인 설정 (localhost는 생략, 배포환경만 설정)
        String domainPart = dynamicDomain != null ? String.format("; Domain=%s", dynamicDomain) : "";
        
        String cookieHeader = String.format("%s=%s; Path=/%s; HttpOnly%s; Max-Age=%d; SameSite=Lax", 
            name, value, domainPart, secureFlag, maxAge);
        
        response.addHeader("Set-Cookie", cookieHeader);
        log.debug("🍪 쿠키 설정: {}, 도메인: {}, HTTPS: {}, 만료시간: {}초", 
                 name, dynamicDomain != null ? dynamicDomain : "자동", isHttps, maxAge);
    }

    /**
     * Access Token을 HttpOnly 쿠키로 설정
     */
    public void setAccessTokenCookie(HttpServletResponse response, HttpServletRequest request, String accessToken) {
        setCookieCommon(response, request, ACCESS_TOKEN_COOKIE_NAME, accessToken, ACCESS_TOKEN_MAX_AGE);
        String domain = determineCookieDomain(request);
        log.info("✅ AccessToken 쿠키 설정 완료 (도메인: {}, HTTPS: {})", 
                domain != null ? domain : "자동", serverDomain.startsWith("https://"));
    }

    /**
     * Pre-Auth Token을 HttpOnly 쿠키로 설정
     */
    public void setPreAuthTokenCookie(HttpServletResponse response, HttpServletRequest request, String preAuthToken) {
        setCookieCommon(response, request, PRE_AUTH_TOKEN_COOKIE_NAME, preAuthToken, PRE_AUTH_TOKEN_MAX_AGE);
        String domain = determineCookieDomain(request);
        log.info("✅ PreAuthToken 쿠키 설정 완료 ({}분 유효, 도메인: {}, HTTPS: {})", 
                PRE_AUTH_TOKEN_MAX_AGE / 60, domain != null ? domain : "자동", serverDomain.startsWith("https://"));
    }

    /**
     * 공통 쿠키 추출 메소드
     */
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    log.debug("🔍 쿠키 발견: {} = {}", cookieName, cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "...");
                    return cookie.getValue();
                }
            }
        }
        log.debug("❌ 쿠키 없음: {}", cookieName);
        return null;
    }

    /**
     * 쿠키에서 Access Token 추출
     */
    public String getAccessTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
    }

    /**
     * 쿠키에서 Pre-Auth Token 추출
     */
    public String getPreAuthTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, PRE_AUTH_TOKEN_COOKIE_NAME);
    }

    /**
     * 공통 쿠키 삭제 메소드 (동적 도메인, HTTPS 감지)
     */
    private void deleteCookieCommon(HttpServletResponse response, HttpServletRequest request, String cookieName) {
        // 🔥 요청 Origin에 따른 동적 도메인 결정
        String dynamicDomain = determineCookieDomain(request);
        
        // HTTPS 환경 감지하여 Secure 플래그 자동 설정
        boolean isHttps = serverDomain.startsWith("https://");
        String secureFlag = isHttps ? "; Secure" : "";
        
        // 도메인 설정 (localhost는 생략, 배포환경만 설정)
        String domainPart = dynamicDomain != null ? String.format("; Domain=%s", dynamicDomain) : "";
        
        String cookieHeader = String.format("%s=; Path=/%s; HttpOnly%s; Max-Age=0; SameSite=Lax", 
            cookieName, domainPart, secureFlag);
        response.addHeader("Set-Cookie", cookieHeader);
        log.info("🗑️ 쿠키 삭제: {} (도메인: {}, HTTPS: {})", 
                cookieName, dynamicDomain != null ? dynamicDomain : "자동", isHttps);
    }

    /**
     * Access Token 쿠키 삭제 (로그아웃)
     */
    public void deleteAccessTokenCookie(HttpServletResponse response, HttpServletRequest request) {
        deleteCookieCommon(response, request, ACCESS_TOKEN_COOKIE_NAME);
    }

    /**
     * Pre-Auth Token 쿠키 삭제
     */
    public void deletePreAuthTokenCookie(HttpServletResponse response, HttpServletRequest request) {
        deleteCookieCommon(response, request, PRE_AUTH_TOKEN_COOKIE_NAME);
    }
    
    /**
     * 요청에 포함된 모든 쿠키 로그 출력 (디버깅용)
     */
    public void logAllCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            log.info("🍪 요청에 포함된 쿠키들:");
            for (Cookie cookie : cookies) {
                log.info("  - {} = {}", cookie.getName(), 
                    cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "...");
            }
        } else {
            log.warn("❌ 요청에 쿠키가 없습니다!");
        }
    }
}