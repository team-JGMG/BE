package org.bobj.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private ObjectMapper objectMapper; // AppConfig에서 정의된 ObjectMapper 사용

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("swagger-ui.html")
            .addResourceLocations("classpath:/META-INF/resources/");

        registry
            .addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
    
    /**
     * Content Negotiation 설정 - JSON을 기본으로 설정
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .favorParameter(false)  // URL 파라미터로 format 지정 비활성화
            .favorPathExtension(false)  // 파일 확장자로 format 지정 비활성화
            .ignoreAcceptHeader(false)  // Accept 헤더 사용
            .defaultContentType(MediaType.APPLICATION_JSON)  // 🔥 기본은 JSON
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("xml", MediaType.APPLICATION_XML);
    }
    
    /**
     * HTTP Message Converter 설정 - JSON을 최우선으로
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // JSON 컨버터를 최우선으로 추가
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setObjectMapper(objectMapper); // AppConfig의 ObjectMapper 사용
        jsonConverter.setSupportedMediaTypes(Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON_UTF8,
            new MediaType("application", "*+json")
        ));
        
        // JSON 컨버터를 맨 앞에 추가 (최우선)
        converters.add(0, jsonConverter);
    }
}
