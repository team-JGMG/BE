package org.bobj.funding.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.event.FundingSuccessEvent;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.funding.mapper.FundingOrderMapper;
import org.bobj.funding.service.ShareDistributionService;
import org.bobj.notification.service.NotificationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Log4j2
@Component
@RequiredArgsConstructor
public class FundingSuccessEventListener {

    private final FundingMapper fundingMapper;
    private final FundingOrderMapper fundingOrderMapper;
    private final NotificationService notificationService;

    private final ShareDistributionService shareDistributionService;


    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFundingSuccessEvent(FundingSuccessEvent event) {
        Long fundingId = event.getFundingId();
        String propertyTitle = fundingMapper.getPropertyTitleByFundingId(fundingId);
        Long ownerUserId = fundingMapper.getUserIdbyFundingId(fundingId);

        // 1. 등록자 알림
        if (ownerUserId != null) {
            String title = "펀딩 성공!";
            String body = "'" + propertyTitle + "' 펀딩이 목표 금액 달성에 성공했습니다. 축하합니다!";
            notificationService.sendNotificationAndSave(ownerUserId, title, body);
        }

        // 2. 참여자 알림
        List<Long> participantUserIds = fundingOrderMapper.findAllOrdersByFundingId(fundingId).stream()
                .map(FundingOrderVO::getUserId)
                .distinct()
                .toList();

        boolean includesUser1 = participantUserIds.contains(1L);
        log.info("펀딩 ID: {} 참여자 수: {}, 유저 ID 1 포함 여부: {}", fundingId, participantUserIds.size(), includesUser1);

        if (!participantUserIds.isEmpty()) {
            String title = "펀딩 성공!";
            String body = "'" + propertyTitle + "' 펀딩이 목표 금액 달성에 성공했습니다. 지분이 곧 분배됩니다.";

            try {
                notificationService.sendBatchNotificationsAndSave(participantUserIds, title, body);
            } catch (Exception e) {
                log.error("펀딩 참여자 알림 전송 실패 - fundingId: {}", fundingId, e);
            }
        }

        log.info("[지분 분배 시작] 펀딩 ID: {}", event.getFundingId());
        shareDistributionService.distributeSharersAsync(event.getFundingId());
    }
}
