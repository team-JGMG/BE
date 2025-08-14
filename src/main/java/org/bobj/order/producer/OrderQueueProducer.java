package org.bobj.order.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class OrderQueueProducer {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ORDER_QUEUE_PREFIX = "order:queue:";

    private static final String ORDER_EVENT_CHANNEL = "order:events";

    public void pushOrder(Long fundingId, Long orderId) {
        String queueKey = ORDER_QUEUE_PREFIX + fundingId;

        redisTemplate.opsForList().leftPush(queueKey, orderId);
        log.info("🛒 주문이 대기 큐에 추가되었습니다. (fundingId={}, orderId={})", fundingId, orderId);

        redisTemplate.convertAndSend(ORDER_EVENT_CHANNEL, String.valueOf(fundingId));
        log.info("📢 Redis Pub/Sub 채널에 주문 발생 이벤트 발행. (channel={}, fundingId={})", ORDER_EVENT_CHANNEL, fundingId);
    }
}
