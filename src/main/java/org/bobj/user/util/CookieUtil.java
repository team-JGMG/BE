package org.bobj.user.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CookieUtil {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String PRE_AUTH_TOKEN_COOKIE_NAME = "preAuthToken";
    private static final int ACCESS_TOKEN_MAX_AGE = 30 * 60; // 30분
    private static final int PRE_AUTH_TOKEN_MAX_AGE = 10 * 60; // 10분

    /**
     * Access Token을 HttpOnly 쿠키로 설정
     */
    public static void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, accessToken);
        cookie.setHttpOnly(true);  // XSS 방지
        cookie.setSecure(false);   // 개발환경에서는 false, 운영환경에서는 true
        cookie.setPath("/");       // 모든 경로에서 접근 가능
        cookie.setMaxAge(ACCESS_TOKEN_MAX_AGE);
        // SameSite는 응답 헤더로 직접 설정
        response.setHeader("Set-Cookie",
                String.format("%s=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Strict",
                        ACCESS_TOKEN_COOKIE_NAME, accessToken, ACCESS_TOKEN_MAX_AGE));
    }

    /**
     * Pre-Auth Token을 HttpOnly 쿠키로 설정
     */
    public static void setPreAuthTokenCookie(HttpServletResponse response, String preAuthToken) {
        response.addHeader("Set-Cookie", 
            String.format("%s=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Strict", 
                PRE_AUTH_TOKEN_COOKIE_NAME, preAuthToken, PRE_AUTH_TOKEN_MAX_AGE));
    }

    /**
     * 쿠키에서 Access Token 추출
     */
    public static String getAccessTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 쿠키에서 Pre-Auth Token 추출
     */
    public static String getPreAuthTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (PRE_AUTH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Access Token 쿠키 삭제 (로그아웃)
     */
    public static void deleteAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);  // 개발환경에서는 false
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 만료

        response.addCookie(cookie);
    }

    /**
     * Pre-Auth Token 쿠키 삭제
     */
    public static void deletePreAuthTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(PRE_AUTH_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);  // 개발환경에서는 false
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 만료

        response.addCookie(cookie);
    }
}