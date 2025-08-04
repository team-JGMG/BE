package org.bobj.order.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderQueueProducer {

    private static final String ORDER_QUEUE_PREFIX = "order:queue:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void pushOrder(Long fundingId, Long orderId) {
        String key = ORDER_QUEUE_PREFIX + fundingId;
        redisTemplate.opsForList().rightPush(key, orderId);
    }
}
