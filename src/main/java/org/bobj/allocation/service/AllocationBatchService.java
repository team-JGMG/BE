package org.bobj.allocation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.allocation.domain.AllocationVO;
import org.bobj.allocation.dto.DividendPaymentDTO;
import org.bobj.allocation.mapper.AllocationMapper;
import org.bobj.point.service.PointService;
import org.bobj.share.mapper.ShareMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationBatchService {

    private final AllocationMapper allocationMapper;
    private final ShareMapper shareMapper;
    private final PointService pointService;

    private static final int BATCH_SIZE = 1000; // 1000명 청크 단위
    private static final int THREAD_POOL_SIZE = 10; // 스레드 풀 크기

    /**
     * 배당 지급일에 대량 배당금 지급 처리 (멀티스레드)
     * 10만명 기준으로 1000명 청크 단위로 처리
     */
    @Transactional
    public void processBatchAllocationPayments(LocalDate paymentDate) {
        log.info("대량 배당금 지급 배치 처리 시작 - 지급일: {}", paymentDate);

        try {
            // 1. 지급일이 도래한 PENDING 상태의 배당금 조회
            List<AllocationVO> pendingAllocations = allocationMapper.findPendingAllocationsForPayment(paymentDate);

            if (pendingAllocations.isEmpty()) {
                log.info("지급 예정인 배당금이 없습니다 - 지급일: {}", paymentDate);
                return;
            }

            log.info("지급 대상 배당금 {}건 발견", pendingAllocations.size());

            // 2. 각 배당금별로 배치 처리
            for (AllocationVO allocation : pendingAllocations) {
                try {
                    processSingleAllocationBatch(allocation);

                    // 배당금 상태를 COMPLETED로 변경
                    allocationMapper.updateAllocationStatus(allocation.getAllocationsId(), "COMPLETED");

                    log.info("배당금 배치 처리 완료 - 배당금 ID: {}, 펀딩 ID: {}",
                            allocation.getAllocationsId(), allocation.getFundingId());

                } catch (Exception e) {
                    log.error("배당금 배치 처리 실패 - 배당금 ID: {}", allocation.getAllocationsId(), e);
                    // 실패한 배당금은 FAILED 상태로 변경
                    allocationMapper.updateAllocationStatus(allocation.getAllocationsId(), "FAILED");
                }
            }

            log.info("대량 배당금 지급 배치 처리 완료 - 총 {}건 처리", pendingAllocations.size());

        } catch (Exception e) {
            log.error("대량 배당금 지급 배치 처리 실패 - 지급일: {}", paymentDate, e);
            throw new RuntimeException("배당금 배치 처리 실패", e);
        }
    }

    /**
     * 단일 배당금에 대한 멀티스레드 배치 처리
     */
    private void processSingleAllocationBatch(AllocationVO allocation) {
        log.info(" 배당금 배치 처리 시작 - 배당금 ID: {}, 펀딩 ID: {}",
                allocation.getAllocationsId(), allocation.getFundingId());

        try {
            // 1. 해당 펀딩의 모든 주식 보유자 조회
            List<DividendPaymentDTO> allShareholders = shareMapper.findShareHoldersByFundingId(allocation.getFundingId());

            if (allShareholders.isEmpty()) {
                log.warn("배당 지급 대상자가 없습니다 - 펀딩 ID: {}", allocation.getFundingId());
                return;
            }

            log.info("총 배당 지급 대상자: {}명", allShareholders.size());

            // 2. 멀티스레드 처리 설정
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            List<Callable<BatchResult>> tasks = new ArrayList<>();

            // 3. 1000명 청크 단위로 작업 분할
            for (int i = 0; i < allShareholders.size(); i += BATCH_SIZE) {
                List<DividendPaymentDTO> shareholderBatch = allShareholders.subList(
                        i, Math.min(i + BATCH_SIZE, allShareholders.size())
                );

                int batchNumber = (i / BATCH_SIZE) + 1;
                tasks.add(() -> processBatchChunk(allocation, shareholderBatch, batchNumber));
            }

            // 4. 멀티스레드 실행 및 결과 수집
            executeBatchTasks(tasks, executor, allocation);

        } catch (Exception e) {
            log.error("배당금 배치 처리 중 오류 - 배당금 ID: {}", allocation.getAllocationsId(), e);
            throw new RuntimeException("배당금 배치 처리 실패", e);
        }
    }

    /**
     * 청크 단위 배당금 지급 처리
     */
    private BatchResult processBatchChunk(AllocationVO allocation, List<DividendPaymentDTO> shareholderChunk, int batchNumber) {
        log.info("배치 청크 처리 시작 - 배당금 ID: {}, 배치 번호: {}, 대상자: {}명",
                allocation.getAllocationsId(), batchNumber, shareholderChunk.size());

        int successCount = 0;
        int failCount = 0;
        BigDecimal totalPaidAmount = BigDecimal.ZERO;

        for (DividendPaymentDTO shareholder : shareholderChunk) {
            try {
                // 개인별 배당금 계산
                shareholder.setDividendPerShare(allocation.getDividendPerShare());
                BigDecimal individualDividend = shareholder.calculateTotalDividend();

                // 배당금 지급
                pointService.allocateDividend(
                        shareholder.getUserId(),
                        individualDividend,
                        allocation.getAllocationsId()
                );

                totalPaidAmount = totalPaidAmount.add(individualDividend);
                successCount++;

                // 대량 처리 시 로그 레벨 조정 (DEBUG로 변경)
                if (log.isDebugEnabled()) {
                    log.debug("배당금 지급 완료 - 사용자 ID: {}, 주식: {}주, 배당금: {}원",
                            shareholder.getUserId(), shareholder.getShareCount(), individualDividend);
                }

            } catch (Exception e) {
                failCount++;
                log.error("개별 배당금 지급 실패 - 사용자 ID: {}, 주식: {}주",
                        shareholder.getUserId(), shareholder.getShareCount(), e);
            }
        }

        BatchResult result = new BatchResult(batchNumber, successCount, failCount, totalPaidAmount);

        log.info("배치 청크 처리 완료 - 배치 번호: {}, 성공: {}명, 실패: {}명, 지급액: {}원",
                batchNumber, successCount, failCount, totalPaidAmount);

        return result;
    }

    /**
     * 배치 작업 실행 및 결과 처리
     */
    private void executeBatchTasks(List<Callable<BatchResult>> tasks, ExecutorService executor, AllocationVO allocation) {
        try {
            log.info("멀티스레드 배치 실행 시작 - 총 {}개 배치, 스레드 풀: {}개",
                    tasks.size(), THREAD_POOL_SIZE);

            List<Future<BatchResult>> futures = executor.invokeAll(tasks);

            // 결과 수집 및 집계
            int totalSuccess = 0;
            int totalFail = 0;
            BigDecimal totalPaidAmount = BigDecimal.ZERO;

            for (Future<BatchResult> future : futures) {
                BatchResult result = future.get();
                totalSuccess += result.getSuccessCount();
                totalFail += result.getFailCount();
                totalPaidAmount = totalPaidAmount.add(result.getTotalPaidAmount());
            }

            log.info("배당금 배치 처리 집계 - 배당금 ID: {}, 총 성공: {}명, 총 실패: {}명, 총 지급액: {}원",
                    allocation.getAllocationsId(), totalSuccess, totalFail, totalPaidAmount);

            // 예상 지급액과 실제 지급액 비교
            if (totalPaidAmount.compareTo(allocation.getTotalDividendAmount()) != 0) {
                log.warn("⚠배당금 지급액 불일치 - 예상: {}원, 실제: {}원",
                        allocation.getTotalDividendAmount(), totalPaidAmount);
            }

        } catch (Exception e) {
            log.error("멀티스레드 배치 실행 실패 - 배당금 ID: {}", allocation.getAllocationsId(), e);
            throw new RuntimeException("배치 실행 실패", e);
        } finally {
            executor.shutdown();
            log.info("ExecutorService 종료 완료");
        }
    }

    /**
     * 배치 처리 결과를 담는 내부 클래스
     */
    private static class BatchResult {
        private final int batchNumber;
        private final int successCount;
        private final int failCount;
        private final BigDecimal totalPaidAmount;

        public BatchResult(int batchNumber, int successCount, int failCount, BigDecimal totalPaidAmount) {
            this.batchNumber = batchNumber;
            this.successCount = successCount;
            this.failCount = failCount;
            this.totalPaidAmount = totalPaidAmount;
        }

        public int getBatchNumber() { return batchNumber; }
        public int getSuccessCount() { return successCount; }
        public int getFailCount() { return failCount; }
        public BigDecimal getTotalPaidAmount() { return totalPaidAmount; }
    }
}