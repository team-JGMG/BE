package org.bobj.user.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * Spring Security를 활성화하고 서블릿 컨테이너에 필터 체인을 등록하는 클래스입니다.
 * 이 클래스를 추가하는 것만으로도 Spring Security가 동작하기 시작합니다.
 */
public class SecurityWebAppInitializer extends AbstractSecurityWebApplicationInitializer {
    // 이 클래스는 특별한 내용을 작성할 필요가 없습니다.
    // 생성자에서 로드할 설정 클래스를 지정할 수는 있지만,
    // RootConfig에서 @Import로 관리하므로 비워둡니다.
}
