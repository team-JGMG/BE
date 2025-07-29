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
     * 공통 쿠키 설정 메소드
     */
    private static void setCookieCommon(HttpServletResponse response, String name, String value, int maxAge) {
        response.addHeader("Set-Cookie", 
            String.format("%s=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Strict", 
                name, value, maxAge));
    }

    /**
     * Access Token을 HttpOnly 쿠키로 설정
     */
    public static void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        setCookieCommon(response, ACCESS_TOKEN_COOKIE_NAME, accessToken, ACCESS_TOKEN_MAX_AGE);
    }

    /**
     * Pre-Auth Token을 HttpOnly 쿠키로 설정
     */
    public static void setPreAuthTokenCookie(HttpServletResponse response, String preAuthToken) {
        setCookieCommon(response, PRE_AUTH_TOKEN_COOKIE_NAME, preAuthToken, PRE_AUTH_TOKEN_MAX_AGE);
    }

    /**
     * 공통 쿠키 추출 메소드
     */
    private static String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 쿠키에서 Access Token 추출
     */
    public static String getAccessTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
    }

    /**
     * 쿠키에서 Pre-Auth Token 추출
     */
    public static String getPreAuthTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, PRE_AUTH_TOKEN_COOKIE_NAME);
    }

    /**
     * 공통 쿠키 삭제 메소드
     */
    private static void deleteCookieCommon(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);  // 개발환경에서는 false
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 만료
        response.addCookie(cookie);
    }

    /**
     * Access Token 쿠키 삭제 (로그아웃)
     */
    public static void deleteAccessTokenCookie(HttpServletResponse response) {
        deleteCookieCommon(response, ACCESS_TOKEN_COOKIE_NAME);
    }

    /**
     * Pre-Auth Token 쿠키 삭제
     */
    public static void deletePreAuthTokenCookie(HttpServletResponse response) {
        deleteCookieCommon(response, PRE_AUTH_TOKEN_COOKIE_NAME);
    }
}