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

    /**
     * 외부 API를 호출하기 위한 RestTemplate을 Bean으로 등록합니다.
     * @return RestTemplate 객체
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    /**
     * JSON 데이터를 Java 객체로 변환하거나 그 반대의 작업을 수행하는 ObjectMapper를 Bean으로 등록합니다.
     * LocalDateTime 등의 Java 8 날짜/시간 타입을 처리할 수 있도록 JavaTimeModule을 등록합니다.
     * @return ObjectMapper 객체
     */
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