package org.bobj.allocation.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.allocation.service.AllocationBatchService;
import org.bobj.allocation.service.AllocationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.LocalDate;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocationScheduler {

    private final AllocationService allocationService;
    private ScheduledExecutorService executorService;
    private final AllocationBatchService allocationBatchService;


    /**
     * 매일 오전 9시에 배당금 지급 처리
     * 오늘 날짜에 지급 예정인 배당금을 처리하고 다음 배당을 생성
     */
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시
    public void processDailyAllocationsBatch() {
        log.info("대량 배당금 지급 배치 스케줄러 시작");

        try {
            LocalDate today = LocalDate.now();
            allocationBatchService.processBatchAllocationPayments(today);

            log.info("대량 배당금 지급 배치 스케줄러 완료");

        } catch (Exception e) {
            log.error("대량 배당금 지급 배치 스케줄러 실행 중 오류 발생", e);
        }
    }

    /**
     * 테스트용 - 즉시 배당금 지급 처리 (개발/테스트 환경에서만 사용)
     */
    public void processAllocationsImmediately() {
        log.info("즉시 배당금 지급 처리 시작 (테스트용)");

        try {
            LocalDate today = LocalDate.now();
            allocationService.processPaymentAndCreateNext(today);

            log.info("즉시 배당금 지급 처리 완료 (테스트용)");

        } catch (Exception e) {
            log.error("즉시 배당금 지급 처리 중 오류 발생 (테스트용)", e);
        }
    }

    /**
     * 애플리케이션 종료 시 스케줄러 정리
     */
    @PreDestroy
    public void destroy() {
        log.info("AllocationScheduler 종료 중...");

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            log.info("ExecutorService 정상 종료됨");
        }

        log.info("AllocationScheduler 종료 완료");
    }


}
