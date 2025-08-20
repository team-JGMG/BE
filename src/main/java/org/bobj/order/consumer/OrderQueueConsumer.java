package org.bobj.order.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.order.domain.OrderVO;
import org.bobj.order.mapper.OrderMapper;
import org.bobj.order.service.OrderMatchingService;
import org.bobj.orderbook.service.OrderBookService;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Log4j2
public class OrderQueueConsumer  implements MessageListener {
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderMapper orderMapper;
    private final OrderMatchingService orderMatchingService;
    private final OrderBookService orderBookService;

    // Pub/Sub 메시지를 처리하는 메서드
    @Override
    @Transactional
    public void onMessage(Message message, byte[] pattern) {
        String fundingIdStr = (String) redisTemplate.getStringSerializer().deserialize(message.getBody());
        if (fundingIdStr != null) {
            fundingIdStr = fundingIdStr.replaceAll("\"", "");
        }
        if (fundingIdStr == null || fundingIdStr.isEmpty()) {
            log.warn("수신된 메시지 내용이 비어있습니다.");
            return;
        }

        Long fundingId = Long.valueOf(fundingIdStr);
        log.info("🎉 새로운 주문 이벤트 수신. 체결 시작 (fundingId={})", fundingId);

        String queueKey = "order:queue:" + fundingId;
        String processingQueueKey = "processing:order:queue:" + fundingId;
        final int LOOP_LIMIT = 500;
        int loop = 0;

        while (true) {
            if (++loop > LOOP_LIMIT) {
                log.warn("루프 상한 도달 → 중단 (fundingId={})", fundingId);
                break;
            }

            Object orderIdObj = redisTemplate.opsForList().rightPopAndLeftPush(queueKey, processingQueueKey);
            if (orderIdObj == null) {
                log.info("큐 비었음 → 종료 (fundingId={})", fundingId);
                break;
            }

            String orderIdStr = orderIdObj.toString();
            try {
                Long orderId = Long.valueOf(orderIdStr);
                OrderVO order = orderMapper.get(orderId);
                if (order == null) {
                    redisTemplate.opsForList().remove(processingQueueKey, 1, orderIdObj);
                    log.warn("주문 없음 → drop (orderId={})", orderId);
                    continue;
                }

                int requested = order.getOrderShareCount();
                int remaining = orderMatchingService.processOrderMatching(order);

                // DB에서 최신 상태 다시 조회 (체결 후 반영된 잔여 수량 확인)
                order = orderMapper.get(orderId);
                remaining = order.getRemainingShareCount();

                // 처리중 큐에서 제거
                redisTemplate.opsForList().remove(processingQueueKey, 1, orderIdObj);

                if (remaining > 0) {
                    // 부분/미체결 → 이번 라운드 재큐잉하지 않음
                    log.info("⏸️ 부분 체결 → 다음 라운드에서 재시도 (orderId={}, remaining={})", orderId, remaining);
                } else {
                    log.info("✅ 완전 체결 → 큐에서 제거 (orderId={})", orderId);
                }

                boolean progressed = (remaining < requested);
                if (!progressed) {
                    log.info("⏸️ 이번 라운드 진전 없음 → 종료 (orderId={})", orderId);
                    break;
                }

            } catch (Exception e) {
                log.error("처리 실패: {}", e.getMessage(), e);
                redisTemplate.opsForList().rightPopAndLeftPush(processingQueueKey, queueKey);
                break;
            }
        }
        // 큐 처리가 모두 끝난 후, 캐시를 무효화합니다.
        orderBookService.evictOrderBookCache(fundingId);
    }

 //매 10초마다 Redis 큐에서 주문 ID 꺼내서 처리
//    @Scheduled(fixedDelay = 10000)
//    public void consumeOrders() {
//        for (Long fundingId : getTrackedFundingIds()) {
//            String queueKey = "order:queue:" + fundingId;
//            String processingQueueKey = "processing:order:queue:" + fundingId;
//
//            Object orderIdObj = redisTemplate.opsForList().rightPopAndLeftPush(queueKey, processingQueueKey);
//            if (orderIdObj == null) continue;
//            try {
//                Long orderId = Long.valueOf(orderIdObj.toString());
//                OrderVO order = orderMapper.get(orderId);
//
//                if (order == null) {
//                    throw new IllegalArgumentException("주문 ID 찾을 수 없음: " + orderId);
//                }
//
//                int remainingCount = orderMatchingService.processOrderMatching(order);
//                orderMatchingService.processOrderMatching(order);
//
//                if (remainingCount > 0) {
//                    // 아직 체결되지 않은 수량이 남아있으면 다시 큐에 넣기
//                    redisTemplate.opsForList().rightPush(queueKey, orderId); // FIFO 유지
//                    log.info("🔁 주문 일부 체결됨. 재큐잉 (orderId={}, remaining={})", orderId, remainingCount);
//                }
//
//                // 처리 완료 시 처리 중 큐에서 주문 ID 삭제
//                redisTemplate.opsForList().remove(processingQueueKey, 1, orderIdObj);
//
//                log.info("✅ 주문 처리 완료 (주문 ID: {})", orderId);
//            } catch (Exception e) {
//                log.error("❗ 주문 처리 실패: {}", e.getMessage());
//
//                // 처리 실패 시, 처리 중 큐에서 다시 원래 큐로 복귀
//                Object popped = redisTemplate.opsForList().rightPopAndLeftPush(processingQueueKey, queueKey);
//            }
//        }
//    }
//
//    private Long[] getTrackedFundingIds() {
//        List<Long> ids = fundingMapper.findAllFundingIds();
//        return ids.toArray(new Long[0]);
//    }

}