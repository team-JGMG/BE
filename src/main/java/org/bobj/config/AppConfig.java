package org.bobj.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 프로젝트 전반에서 사용될 공용 Bean들을 등록하는 설정 클래스
 */
@Configuration
public class AppConfig {

    //외부 API를 호출하기 위한 RestTemplate을 Bean으로 등록. @return RestTemplate
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    //JSON 데이터, Java 객체 사이 변환 ObjectMapper를 Bean 등록.
    //LocalDateTime 등의 Java 8 날짜/시간 타입을 처리 JavaTimeModule
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Java 8 날짜/시간 타입 지원 모듈 등록
        mapper.registerModule(new JavaTimeModule());
        // 필요에 따라 timestamp 대신 ISO-8601 형식 출력 설정 가능

        // mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}