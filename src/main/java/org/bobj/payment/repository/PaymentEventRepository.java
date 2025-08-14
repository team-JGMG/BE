package org.bobj.payment.repository;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.bobj.payment.domain.PaymentEventVO;
import org.bobj.payment.mapper.PaymentEventMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentEventRepository {
    private final PaymentEventMapper mapper;

    public boolean existsByMerchantUidAndEventTypeAndPgEventAt(String merchantUid, String eventType, LocalDateTime pgEventAt) {
        return mapper.existsByMerchantUidAndEventTypeAndPgEventAt(merchantUid, eventType, pgEventAt);
    }

    public void save(PaymentEventVO vo) {
        mapper.save(vo);
    }
}
