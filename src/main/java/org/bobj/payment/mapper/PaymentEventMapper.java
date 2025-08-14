package org.bobj.payment.mapper;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.payment.domain.PaymentEventVO;

@Mapper
public interface PaymentEventMapper {


        boolean existsByMerchantUidAndEventTypeAndPgEventAt(
            @Param("merchantUid") String merchantUid,
            @Param("eventType") String eventType,
            @Param("pgEventAt") LocalDateTime pgEventAt);

        void save(PaymentEventVO vo);
}

