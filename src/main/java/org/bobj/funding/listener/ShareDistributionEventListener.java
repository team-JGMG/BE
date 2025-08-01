package org.bobj.funding.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.funding.event.ShareDistributionEvent;
import org.bobj.funding.service.ShareDistributionService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShareDistributionEventListener {

    private final ShareDistributionService shareDistributionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleShareDistribution(ShareDistributionEvent event) {
        log.info("[지분 분배 시작] 펀딩 ID: {}", event.getFundingId());
        shareDistributionService.distributeSharersAsync(event.getFundingId());
    }
}
