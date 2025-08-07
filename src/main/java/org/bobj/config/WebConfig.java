package org.bobj.config;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class WebConfig extends AbstractAnnotationConfigDispatcherServletInitializer {


    // 루트 설정 클래스 (DB, 보안 등 전역 설정)
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{RootConfig.class,  AsyncConfig.class, RedisConfig.class};
    }


    // 서블릿 관련 설정 클래스
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{
                SwaggerConfig.class,
                ServletConfig.class,
                SecurityConfig.class,
                WebSocketConfig.class
        };
    }

    // 스프링의 FrontController인 DispatcherServlet이 담당할 Url 매핑 패턴, / : 모든 요청에 대해 매핑
    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    // POST body 문자 인코딩 필터 설정 - UTF-8 설정
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding("UTF-8");
        characterEncodingFilter.setForceEncoding(true);
        return new Filter[]{characterEncodingFilter};
    }

//    @Value("${multipart.upload.location:/tmp}")
//    private String uploadLocation = "tmp";
    private String uploadLocation = "C:/temp";

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        MultipartConfigElement multipartConfigElement =
                new MultipartConfigElement(uploadLocation, 52428800, 209715200, 0);
        registration.setMultipartConfig(multipartConfigElement);
    }
}
