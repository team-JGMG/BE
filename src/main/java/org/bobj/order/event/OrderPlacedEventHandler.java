package org.bobj.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.order.producer.OrderQueueProducer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Log4j2
public class OrderPlacedEventHandler {
    private final OrderQueueProducer orderQueueProducer;       // Redis 리스트 push

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderPlacedEvent e) {
        orderQueueProducer.pushOrder(e.getFundingId(), e.getOrderId());
    }
}