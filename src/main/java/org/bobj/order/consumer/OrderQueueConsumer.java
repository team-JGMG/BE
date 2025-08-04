package org.bobj.order.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.order.domain.OrderVO;
import org.bobj.order.mapper.OrderMapper;
import org.bobj.order.service.OrderMatchingService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class OrderQueueConsumer {
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderMapper orderMapper;
    private final OrderMatchingService orderMatchingService;
    private final FundingMapper fundingMapper;

    // 매 1초마다 Redis 큐에서 주문 ID 꺼내서 처리
    @Scheduled(fixedDelay = 1000)
    public void consumeOrders() {
        for (Long fundingId : getTrackedFundingIds()) {
            String queueKey = "order:queue:" + fundingId;
            String processingQueueKey = "processing:order:queue:" + fundingId;

            Object orderIdObj = redisTemplate.opsForList().rightPopAndLeftPush(queueKey, processingQueueKey);
            if (orderIdObj == null) continue;
            try {
                Long orderId = Long.valueOf(orderIdObj.toString());
                OrderVO order = orderMapper.get(orderId);

                if (order == null) {
                    throw new IllegalArgumentException("주문 ID 찾을 수 없음: " + orderId);
                }

                int remainingCount = orderMatchingService.processOrderMatching(order);
//                orderMatchingService.processOrderMatching(order);

                if (remainingCount > 0) {
                    // 아직 체결되지 않은 수량이 남아있으면 다시 큐에 넣기
                    redisTemplate.opsForList().rightPush(queueKey, orderId); // FIFO 유지
                    log.info("🔁 주문 일부 체결됨. 재큐잉 (orderId={}, remaining={})", orderId, remainingCount);
                }

                // 처리 완료 시 처리 중 큐에서 주문 ID 삭제
                redisTemplate.opsForList().remove(processingQueueKey, 1, orderIdObj);

                log.info("✅ 주문 처리 완료 (주문 ID: {})", orderId);
            } catch (Exception e) {
                log.error("❗ 주문 처리 실패: {}", e.getMessage());

                // 처리 실패 시, 처리 중 큐에서 다시 원래 큐로 복귀
                Object popped = redisTemplate.opsForList().rightPopAndLeftPush(processingQueueKey, queueKey);
            }
        }
    }

    private Long[] getTrackedFundingIds() {
        List<Long> ids = fundingMapper.findAllFundingIds();
        return ids.toArray(new Long[0]);
    }
}
