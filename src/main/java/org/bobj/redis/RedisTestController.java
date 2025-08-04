package org.bobj.redis; // ← 경로에 맞게 수정

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class RedisTestController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping("/redis-test")
    public String testRedis() {
        redisTemplate.opsForValue().set("testKey", "hello redis", Duration.ofMinutes(1));
        return redisTemplate.opsForValue().get("testKey");
    }
}
