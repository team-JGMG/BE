package org.example.config;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.example.bobj.config.RootConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;


@Slf4j
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = RootConfig.class)
@ActiveProfiles("test")

class RootConfigTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PropertySourcesPlaceholderConfigurer configurer;

    @Autowired
    private RootConfig rootConfig;


    @Test
    @DisplayName("DataSource가 정상적으로 연결된다.")
    void dataSource() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            assertNotNull(conn);
            log.info("DB 연결 성공: {}", conn);
        }
    }

    @Test
    @DisplayName("PropertySourcesPlaceholderConfigurer 빈이 정상 생성된다.")
    void propertyConfig() {
        assertNotNull(configurer, "PropertySourcesPlaceholderConfigurer가 null입니다.");
        log.info("propertyConfig() 정상 생성됨: {}", configurer.getClass().getSimpleName());
    }

    @Test
    @DisplayName("@Value 설정 값이 주입되어 있다.")
    void checkInjectedValues() throws Exception {
        String[] fieldNames = { "driver", "url", "username", "password" };

        for (String fieldName : fieldNames) {
            var field = RootConfig.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            var value = field.get(rootConfig);
            assertNotNull(value, fieldName + " 주입 실패");
            log.info("{} = {}", fieldName, value);
        }
    }
}