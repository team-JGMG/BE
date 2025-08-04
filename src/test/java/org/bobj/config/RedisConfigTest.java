package org.bobj.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = RedisConfig.class)
@ActiveProfiles("test")
class RedisConfigTest {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private PropertySourcesPlaceholderConfigurer configurer;

    @Test
    @DisplayName("RedisConnectionFactory가 정상적으로 주입된다.")
    void testRedisConnectionFactory() {
        assertNotNull(redisConnectionFactory, "RedisConnectionFactory가 null입니다.");

        RedisConnection connection = redisConnectionFactory.getConnection();
        assertNotNull(connection, "Redis 연결이 실패했습니다.");
        log.info("Redis 연결 성공: {}", connection);
    }

    @Test
    @DisplayName("RedisTemplate이 정상적으로 생성된다.")
    void testRedisTemplate() {
        assertNotNull(redisTemplate, "RedisTemplate이 null입니다.");
        redisTemplate.opsForValue().set("testKey", "Hello Redis");
        Object value = redisTemplate.opsForValue().get("testKey");
        assertEquals("Hello Redis", value);
        log.info("RedisTemplate 동작 확인: testKey = {}", value);
    }

//    @Test
//    @DisplayName("PropertySourcesPlaceholderConfigurer가 정상 주입된다.")
//    void testPropertySourcesPlaceholderConfigurer() {
//        assertNotNull(configurer, "PropertySourcesPlaceholderConfigurer가 null입니다.");
//        log.info("PropertySourcesPlaceholderConfigurer: {}", configurer.getClass().getSimpleName());
//    }
}
