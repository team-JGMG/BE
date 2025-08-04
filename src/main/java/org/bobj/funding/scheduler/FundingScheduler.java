package org.bobj.funding.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.funding.service.FundingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class FundingScheduler {
    private final FundingService fundingService;

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정 실행
    public void runFundingFailJob(){
        fundingService.expireFunding();
    }
}
