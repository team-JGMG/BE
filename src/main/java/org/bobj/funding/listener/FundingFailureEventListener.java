package org.bobj.funding.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.event.FundingFailureEvent;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.funding.mapper.FundingOrderMapper;
import org.bobj.notification.service.NotificationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundingFailureEventListener {

    private final FundingMapper fundingMapper;
    private final FundingOrderMapper fundingOrderMapper;
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFundingFailureEvent(FundingFailureEvent event) {
        Long fundingId = event.getFundingId();

        log.info("[펀딩 실패 이벤트 수신] fundingId: {}", fundingId);
        String propertyTitle = fundingMapper.getPropertyTitleByFundingId(fundingId);
        Long ownerUserId = fundingMapper.getUserIdbyFundingId(fundingId);

        // 1. 등록자 알림
        if (ownerUserId != null) {
            String title = "펀딩이 실패되었어요!";
            String body = "'" + propertyTitle + "' 펀딩이 목표 달성 실패로 마감되었습니다.";
            notificationService.sendNotificationAndSave(ownerUserId, title, body);
        }

        // 2. 참여자 알림
        List<FundingOrderVO> allOrders = fundingOrderMapper.findAllOrdersByFundingId(fundingId);
        if (!allOrders.isEmpty()) {
            List<Long> participantUserIds = allOrders.stream()
                    .map(FundingOrderVO::getUserId)
                    .distinct()
                    .toList();

            String title = "펀딩이 실패되었어요!";
            String body = "'" + propertyTitle + "' 펀딩이 목표 금액을 달성하지 못해 마감되었습니다. "
                    + "결제 포인트는 순차적으로 환불 처리되며, 환불 완료까지 시간이 소요될 수 있습니다.";

            try {
                notificationService.sendBatchNotificationsAndSave(participantUserIds, title, body);
            } catch (Exception e) {
                log.error("펀딩 참여자 알림 전송 실패 - fundingId: {}", fundingId, e);
            }
        }
    }
}
