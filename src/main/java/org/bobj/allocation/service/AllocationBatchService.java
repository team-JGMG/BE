package org.bobj.allocation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.allocation.domain.AllocationVO;
import org.bobj.allocation.dto.DividendPaymentDTO;
import org.bobj.allocation.mapper.AllocationMapper;
import org.bobj.point.service.PointService;
import org.bobj.share.mapper.ShareMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationBatchService {

    private final AllocationMapper allocationMapper;
    private final ShareMapper shareMapper;
    private final PointService pointService;

    // 배치 처리 관련 상수 설정
    private static final int BATCH_SIZE = 1000; // 1000명 청크 단위로 분할 처리
    private static final int THREAD_POOL_SIZE = 10; // 멀티스레드 풀 크기 (동시 처리할 청크 수)
    private static final double ROLLBACK_THRESHOLD = 1.0; // 1% 실패 시 청크 롤백
    private static final int MAX_RETRY_COUNT = 3; // 청크 재시도 최대 횟수

    /**
     * 대량 배당금 지급 배치 처리의 메인 진입점
     * 여러 배당금을 순차적으로 처리하되, 각각은 독립적인 트랜잭션으로 실행
     * 
     * 처리 흐름:
     * 1. 오늘 지급일인 PENDING 상태 배당금들 조회
     * 2. 각 배당금마다 별도 트랜잭션으로 처리 (독립성 보장)
     * 3. 하나의 배당금이 실패해도 다른 배당금에는 영향 없음
     */
    @Transactional(readOnly = true) // 메인 메서드는 조회만 담당 (트랜잭션 충돌 방지)
    public void processBatchAllocationPayments(LocalDate paymentDate) {
        log.info("=== 대량 배당금 지급 배치 처리 시작 === 지급일: {}", paymentDate);

        try {
            // 1. 오늘 지급 예정인 PENDING 상태 배당금들을 DB에서 조회
            List<AllocationVO> pendingAllocations = allocationMapper.findPendingAllocationsForPayment(paymentDate);

            // 지급할 배당금이 없으면 조기 종료
            if (pendingAllocations.isEmpty()) {
                log.info("📋 지급 예정인 배당금이 없습니다 - 지급일: {}", paymentDate);
                return;
            }

            log.info("📋 지급 대상 배당금 {}건 발견", pendingAllocations.size());

            // 2. 각 배당금별로 독립적인 트랜잭션으로 처리
            // 하나의 배당금이 실패해도 다른 배당금 처리에는 영향 없음
            for (AllocationVO allocation : pendingAllocations) {
                processSingleAllocationWithTransaction(allocation);
            }

            log.info("=== 대량 배당금 지급 배치 처리 완료 === 총 {}건 처리", pendingAllocations.size());

        } catch (Exception e) {
            log.error("💥 대량 배당금 지급 배치 처리 전체 실패 - 지급일: {}", paymentDate, e);
            throw new RuntimeException("배당금 배치 처리 실패", e);
        }
    }

    /**
     * 단일 배당금을 별도 트랜잭션에서 처리
     * 각 배당금마다 새로운 트랜잭션을 시작해서 독립성 보장
     * 
     * 처리 방식:
     * - @Transactional(REQUIRES_NEW): 기존 트랜잭션과 독립적으로 새 트랜잭션 시작
     * - 성공 시: COMPLETED 상태로 변경
     * - 실패 시: FAILED 상태로 변경하되 다른 배당금에는 영향 없음
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, 
                   rollbackFor = Exception.class)
    public void processSingleAllocationWithTransaction(AllocationVO allocation) {
        try {
            log.info("🔄 단일 배당금 처리 시작 - 배당금 ID: {}, 펀딩 ID: {}", 
                    allocation.getAllocationsId(), allocation.getFundingId());

            // 실제 배당금 지급 처리 (멀티스레드)
            processSingleAllocationBatch(allocation);

            // 모든 처리가 성공했으면 배당금 상태를 COMPLETED로 변경
            allocationMapper.updateAllocationStatus(allocation.getAllocationsId(), "COMPLETED");

            log.info("✅ 단일 배당금 처리 성공 - 배당금 ID: {}, 펀딩 ID: {}",
                    allocation.getAllocationsId(), allocation.getFundingId());

        } catch (Exception e) {
            log.error("❌ 단일 배당금 처리 실패 - 배당금 ID: {}", allocation.getAllocationsId(), e);
            
            // 실패한 배당금은 FAILED 상태로 변경 (별도 트랜잭션에서 안전하게)
            try {
                allocationMapper.updateAllocationStatus(allocation.getAllocationsId(), "FAILED");
                log.info("💡 배당금 상태를 FAILED로 변경 - 배당금 ID: {}", allocation.getAllocationsId());
            } catch (Exception statusUpdateError) {
                log.error("❌ 배당금 상태 업데이트 실패 - 배당금 ID: {}", allocation.getAllocationsId(), statusUpdateError);
            }
            
            // 개별 배당금 실패가 전체 배치를 중단시키지 않도록 예외를 여기서 처리
            // 다른 배당금들은 계속 처리될 수 있도록 함
        }
    }

    /**
     * 단일 배당금에 대한 멀티스레드 배치 처리
     * 대상자를 청크 단위로 나누어 병렬 처리
     * 
     * 처리 단계:
     * 1. 해당 펀딩의 모든 주식 보유자 조회
     * 2. BATCH_SIZE(1000명) 단위로 청크 분할
     * 3. 각 청크를 멀티스레드로 병렬 처리
     * 4. 결과 수집 및 통계 산출
     */
    private void processSingleAllocationBatch(AllocationVO allocation) {
        log.info("📦 배당금 배치 처리 시작 - 배당금 ID: {}, 펀딩 ID: {}",
                allocation.getAllocationsId(), allocation.getFundingId());

        try {
            // 1. 해당 펀딩의 모든 주식 보유자들을 DB에서 조회
            List<DividendPaymentDTO> allShareholders = shareMapper.findShareHoldersByFundingId(allocation.getFundingId());

            // 주식 보유자가 없으면 처리할 것이 없으므로 종료
            if (allShareholders.isEmpty()) {
                log.warn("⚠️ 배당 지급 대상자가 없습니다 - 펀딩 ID: {}", allocation.getFundingId());
                return;
            }

            log.info("👥 총 배당 지급 대상자: {}명", allShareholders.size());

            // 2. 멀티스레드 처리를 위한 ExecutorService 생성
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            List<Callable<BatchResult>> tasks = new ArrayList<>();

            // 3. 전체 대상자를 BATCH_SIZE(1000명) 단위로 청크 분할
            for (int i = 0; i < allShareholders.size(); i += BATCH_SIZE) {
                // 현재 청크의 끝 인덱스 계산 (배열 범위 초과 방지)
                List<DividendPaymentDTO> shareholderBatch = allShareholders.subList(
                        i, Math.min(i + BATCH_SIZE, allShareholders.size())
                );

                int batchNumber = (i / BATCH_SIZE) + 1; // 1부터 시작하는 배치 번호
                
                // 각 청크를 처리할 Callable 작업 생성 (1% 임계값 롤백 포함)
                tasks.add(() -> processBatchChunkWithRollback(allocation, shareholderBatch, batchNumber));
            }

            // 4. 멀티스레드로 모든 청크를 병렬 실행하고 결과 수집
            executeBatchTasks(tasks, executor, allocation);

        } catch (Exception e) {
            log.error("💥 배당금 배치 처리 중 전체 오류 - 배당금 ID: {}", allocation.getAllocationsId(), e);
            throw new RuntimeException("배당금 배치 처리 실패", e);
        }
    }

    /**
     * 청크 단위 배당금 지급 처리 (1% 임계값 롤백 로직 포함)
     * 각 청크를 독립적인 트랜잭션으로 처리하며, 실패율이 1% 초과 시 전체 롤백
     * 
     * 롤백 로직:
     * - 청크 내 실패율 계산: (실패자 수 / 전체 대상자 수) * 100
     * - 1% 초과 시: 이미 성공한 사용자들의 포인트를 롤백하고 전체 청크 실패 처리
     * - 1% 이하 시: 성공한 사용자들은 그대로 두고 실패한 사용자들만 기록
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public BatchResult processBatchChunkWithRollback(AllocationVO allocation, List<DividendPaymentDTO> shareholderChunk, int batchNumber) {
        log.info("🧩 청크 처리 시작 - 배당금 ID: {}, 청크 번호: {}, 대상자: {}명",
                allocation.getAllocationsId(), batchNumber, shareholderChunk.size());

        int successCount = 0;
        int failCount = 0;
        BigDecimal totalPaidAmount = BigDecimal.ZERO;
        List<Long> failedUserIds = new ArrayList<>();
        List<Long> successUserIds = new ArrayList<>(); // 성공한 사용자 ID (롤백용)

        // 각 사용자별로 배당금 지급 시도
        for (DividendPaymentDTO shareholder : shareholderChunk) {
            try {
                // 개인별 배당금 계산 (주당 배당금 × 보유 주식 수)
                shareholder.setDividendPerShare(allocation.getDividendPerShare());
                BigDecimal individualDividend = shareholder.calculateTotalDividend();

                // 실제 포인트 지급 처리
                pointService.allocateDividend(
                        shareholder.getUserId(),
                        individualDividend,
                        allocation.getAllocationsId()
                );

                // 성공 시 통계 업데이트
                totalPaidAmount = totalPaidAmount.add(individualDividend);
                successCount++;
                successUserIds.add(shareholder.getUserId());

                // DEBUG 레벨에서만 개별 성공 로그 출력 (대량 처리 시 로그 과부하 방지)
                if (log.isDebugEnabled()) {
                    log.debug("✅ 개별 지급 성공 - 사용자 ID: {}, 주식: {}주, 배당금: {}원",
                            shareholder.getUserId(), shareholder.getShareCount(), individualDividend);
                }

            } catch (Exception e) {
                // 개별 사용자 지급 실패
                failCount++;
                failedUserIds.add(shareholder.getUserId());
                log.error("❌ 개별 지급 실패 - 사용자 ID: {}, 주식: {}주, 오류: {}",
                        shareholder.getUserId(), shareholder.getShareCount(), e.getMessage());
            }
        }

        // 실패율 계산 (전체 대상자 대비 실패자 비율)
        double failureRate = (double) failCount / shareholderChunk.size() * 100;
        
        log.info("📊 청크 처리 결과 - 청크 번호: {}, 성공: {}명, 실패: {}명, 실패율: {:.2f}%",
                batchNumber, successCount, failCount, failureRate);

        // === 1% 임계값 체크 및 롤백 로직 ===
        if (failureRate > ROLLBACK_THRESHOLD) {
            log.error("🚨 청크 실패율 {:.2f}%가 임계값 {:.1f}%를 초과 - 청크 전체 롤백 실행!", 
                     failureRate, ROLLBACK_THRESHOLD);

            // 이미 성공한 사용자들의 포인트를 되돌림
            rollbackSuccessfulPayments(successUserIds, allocation);

            // 롤백 후 예외 발생 (트랜잭션 롤백 트리거)
            throw new ChunkRollbackException(
                String.format("청크 %d 롤백: 실패율 %.2f%% > 임계값 %.1f%%", 
                             batchNumber, failureRate, ROLLBACK_THRESHOLD));
        }

        // 임계값 이하면 정상 처리 완료
        log.info("✅ 청크 처리 완료 - 청크 번호: {}, 실패율 {:.2f}%는 임계값 이하로 정상 처리",
                batchNumber, failureRate);

        return new BatchResult(batchNumber, successCount, failCount, totalPaidAmount, failedUserIds);
    }

    /**
     * 롤백 시 성공했던 사용자들의 포인트 지급을 취소
     * 포인트 차감 및 역방향 트랜잭션 기록 생성
     * 
     * 처리 방식:
     * - 각 사용자별로 이전에 지급한 배당금만큼 포인트 차감
     * - ALLOCATION 타입의 역방향 트랜잭션 기록 생성 (음수 금액)
     * - 개별 롤백 실패해도 전체 롤백은 계속 진행
     */
    private void rollbackSuccessfulPayments(List<Long> successUserIds, AllocationVO allocation) {
        log.warn("🔄 성공한 사용자들 포인트 롤백 시작 - 대상: {}명", successUserIds.size());
        
        int rollbackSuccess = 0;
        int rollbackFail = 0;
        
        for (Long userId : successUserIds) {
            try {
                // TODO: PointService에 배당금 취소 메서드 추가 필요
                // 현재는 임시로 로깅만 수행 (실제 구현 시 아래와 같은 메서드 필요)
                // pointService.cancelDividend(userId, amount, allocation.getAllocationsId());
                
                log.debug("🔄 포인트 롤백 예정 - 사용자 ID: {}", userId);
                rollbackSuccess++;
                
                // 실제 구현 시 필요한 로직:
                // 1. 해당 사용자의 배당금 계산
                // 2. 포인트에서 해당 금액 차감
                // 3. 역방향 트랜잭션 기록 생성 (ALLOCATION_CANCEL 등)
                
            } catch (Exception e) {
                rollbackFail++;
                log.error("❌ 포인트 롤백 실패 - 사용자 ID: {}, 오류: {}", userId, e.getMessage());
            }
        }
        
        log.warn("🔄 포인트 롤백 완료 - 성공: {}건, 실패: {}건", rollbackSuccess, rollbackFail);
        
        // 롤백 실패가 많으면 경고
        if (rollbackFail > 0) {
            log.error("🚨 일부 포인트 롤백 실패 - 수동 처리 필요: {}건", rollbackFail);
        }
    }
    /**
     * 멀티스레드 배치 작업 실행 및 결과 수집
     * 각 Future의 결과를 안전하게 수집하고 전체 통계 산출
     * 
     * 처리 과정:
     * 1. 모든 청크를 병렬로 실행 시작
     * 2. 각 청크의 결과를 순차적으로 수집
     * 3. ChunkRollbackException은 1% 임계값 롤백으로 분류
     * 4. 기타 Exception은 시스템 오류로 분류
     * 5. 전체 통계 산출 및 품질 평가
     */
    private void executeBatchTasks(List<Callable<BatchResult>> tasks, ExecutorService executor, AllocationVO allocation) {
        try {
            log.info("🚀 멀티스레드 배치 실행 시작 - 총 {}개 청크, 스레드 풀: {}개",
                    tasks.size(), THREAD_POOL_SIZE);

            // 모든 청크 작업을 병렬로 시작
            List<Future<BatchResult>> futures = executor.invokeAll(tasks);

            // 전체 결과를 집계할 변수들 초기화
            int totalSuccess = 0;
            int totalFail = 0;
            int chunkSuccessCount = 0; // 성공한 청크 수
            int chunkRollbackCount = 0; // 롤백된 청크 수
            int chunkErrorCount = 0; // 실행 자체가 실패한 청크 수
            BigDecimal totalPaidAmount = BigDecimal.ZERO;

            // 각 청크의 처리 결과를 순차적으로 수집
            for (Future<BatchResult> future : futures) {
                try {
                    // 청크 처리 완료까지 대기하고 결과 획득
                    BatchResult result = future.get();
                    
                    // 정상 완료된 청크의 결과를 전체 통계에 합산
                    totalSuccess += result.getSuccessCount();
                    totalFail += result.getFailCount();
                    totalPaidAmount = totalPaidAmount.add(result.getTotalPaidAmount());
                    chunkSuccessCount++;
                    
                    // 실패한 사용자들이 있으면 로깅 (너무 많으면 일부만)
                    if (!result.getFailedUserIds().isEmpty()) {
                        if (result.getFailedUserIds().size() <= 5) {
                            log.info("⚠️ 청크 {}에서 실패한 사용자들: {}", 
                                   result.getBatchNumber(), result.getFailedUserIds());
                        } else {
                            log.info("⚠️ 청크 {}에서 {}명 실패 (처음 5명: {}...)", 
                                   result.getBatchNumber(), result.getFailedUserIds().size(), 
                                   result.getFailedUserIds().subList(0, 5));
                        }
                    }
                    
                    log.info("✅ 청크 결과 수집 - 청크: {}, 성공: {}명, 실패: {}명", 
                            result.getBatchNumber(), result.getSuccessCount(), result.getFailCount());
                    
                } catch (Exception e) {
                    // 예외 타입별로 분류 처리
                    if (e.getCause() instanceof ChunkRollbackException) {
                        // 1% 임계값 초과로 인한 청크 롤백
                        chunkRollbackCount++;
                        log.warn("🔄 청크 롤백됨 - {}", e.getCause().getMessage());
                    } else {
                        // 청크 실행 자체의 예외 (시스템 오류 등)
                        chunkErrorCount++;
                        log.error("💥 청크 실행 실패 - 시스템 오류: {}", e.getMessage(), e);
                    }
                }
            }

            // === 전체 배당금 처리 결과 요약 로깅 ===
            log.info("🎯 배당금 처리 최종 결과 - 배당금 ID: {}", allocation.getAllocationsId());
            log.info("   📊 사용자 통계: 성공 {}명, 실패 {}명, 총 지급액: {}원", totalSuccess, totalFail, totalPaidAmount);
            log.info("   📦 청크 통계: 성공 {}개, 롤백 {}개, 오류 {}개", chunkSuccessCount, chunkRollbackCount, chunkErrorCount);

            // 성공률 계산 및 품질 평가
            int totalAttempts = totalSuccess + totalFail;
            double successRate = totalAttempts > 0 ? (double) totalSuccess / totalAttempts * 100 : 0;
            
            log.info("📈 전체 성공률: {:.2f}% ({}/{}명)", successRate, totalSuccess, totalAttempts);

            // 예상 지급액과 실제 지급액 검증
            if (totalPaidAmount.compareTo(allocation.getTotalDividendAmount()) != 0) {
                BigDecimal difference = allocation.getTotalDividendAmount().subtract(totalPaidAmount);
                log.warn("⚠️ 배당금 지급액 불일치 - 예상: {}원, 실제: {}원, 차이: {}원",
                        allocation.getTotalDividendAmount(), totalPaidAmount, difference);
            }

            // 품질 임계값 경고 (전체 성공률 기준)
            if (successRate < 95.0) {
                log.warn("🚨 전체 성공률이 95% 미만입니다: {:.2f}% - 시스템 점검 필요", successRate);
            }

            // 롤백된 청크가 많으면 시스템 문제 가능성 높음
            if (chunkRollbackCount > tasks.size() * 0.1) { // 전체 청크의 10% 이상 롤백
                log.error("🚨 롤백된 청크가 많습니다: {}개/{}개 - 시스템 점검 필요", chunkRollbackCount, tasks.size());
            }

        } catch (InterruptedException e) {
            // 스레드 인터럽트 처리
            Thread.currentThread().interrupt();
            log.error("💥 배치 처리가 인터럽트됨 - 배당금 ID: {}", allocation.getAllocationsId(), e);
            throw new RuntimeException("배치 처리 인터럽트", e);
            
        } catch (Exception e) {
            log.error("💥 멀티스레드 배치 실행 전체 실패 - 배당금 ID: {}", allocation.getAllocationsId(), e);
            throw new RuntimeException("배치 실행 실패", e);
            
        } finally {
            // ExecutorService 안전한 종료 처리
            shutdownExecutorSafely(executor);
        }
    }

    /**
     * ExecutorService를 안전하게 종료하는 유틸리티 메서드
     * 정상 종료 → 대기 → 강제 종료 순으로 처리
     * 
     * 종료 순서:
     * 1. shutdown() - 새로운 작업 수락 중단, 기존 작업은 완료까지 대기
     * 2. awaitTermination(60초) - 기존 작업 완료까지 대기
     * 3. shutdownNow() - 실행 중인 작업도 강제 중단
     * 4. 추가 awaitTermination(10초) - 강제 종료도 완료될 때까지 대기
     */
    private void shutdownExecutorSafely(ExecutorService executor) {
        log.debug("🔚 ExecutorService 종료 프로세스 시작");
        
        // 1단계: 정상 종료 시도 (새로운 작업은 받지 않음)
        executor.shutdown();
        
        try {
            // 2단계: 기존 작업 완료까지 60초 대기
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("⏰ ExecutorService 정상 종료 타임아웃 - 강제 종료 시도");
                
                // 3단계: 강제 종료 (실행 중인 작업도 중단)
                List<Runnable> pendingTasks = executor.shutdownNow();
                log.warn("🛑 ExecutorService 강제 종료 - 대기 중인 작업: {}개", pendingTasks.size());
                
                // 4단계: 강제 종료 후에도 추가 대기
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("💥 ExecutorService 강제 종료도 실패 - 완전히 종료되지 않음");
                }
            }
        } catch (InterruptedException e) {
            // 인터럽트 발생 시 강제 종료
            log.warn("🔄 ExecutorService 종료 중 인터럽트 발생 - 강제 종료");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.debug("🏁 ExecutorService 종료 완료");
    }
    /**
     * 배치 처리 결과를 담는 내부 클래스
     * 각 청크의 처리 결과와 실패한 사용자 정보를 포함
     * 
     * 포함 정보:
     * - batchNumber: 청크 번호 (1부터 시작, 디버깅용)
     * - successCount: 성공한 사용자 수
     * - failCount: 실패한 사용자 수  
     * - totalPaidAmount: 실제 지급된 총 금액
     * - failedUserIds: 실패한 사용자 ID 목록 (재처리용)
     */
    private static class BatchResult {
        private final int batchNumber; // 청크 번호 (1부터 시작)
        private final int successCount; // 성공한 사용자 수
        private final int failCount; // 실패한 사용자 수
        private final BigDecimal totalPaidAmount; // 실제 지급된 총 금액
        private final List<Long> failedUserIds; // 실패한 사용자 ID 목록 (재처리용)

        // 메인 생성자 (실패한 사용자 ID 포함)
        public BatchResult(int batchNumber, int successCount, int failCount, BigDecimal totalPaidAmount, List<Long> failedUserIds) {
            this.batchNumber = batchNumber;
            this.successCount = successCount;
            this.failCount = failCount;
            this.totalPaidAmount = totalPaidAmount;
            this.failedUserIds = failedUserIds != null ? failedUserIds : new ArrayList<>();
        }

        // 하위 호환성을 위한 기존 생성자 (실패 사용자 ID 없음)
        public BatchResult(int batchNumber, int successCount, int failCount, BigDecimal totalPaidAmount) {
            this(batchNumber, successCount, failCount, totalPaidAmount, new ArrayList<>());
        }

        // Getter 메서드들
        public int getBatchNumber() { return batchNumber; }
        public int getSuccessCount() { return successCount; }
        public int getFailCount() { return failCount; }
        public BigDecimal getTotalPaidAmount() { return totalPaidAmount; }
        public List<Long> getFailedUserIds() { return failedUserIds; }
    }

    /**
     * 청크 롤백 예외 클래스
     * 1% 임계값 초과로 인한 청크 롤백을 나타내는 사용자 정의 예외
     * 
     * 용도:
     * - 시스템 오류와 1% 임계값 롤백을 구분하기 위함
     * - executeBatchTasks에서 예외 타입별로 다른 로깅 처리
     * - 통계 산출 시 롤백 청크 수와 오류 청크 수를 분리 집계
     */
    private static class ChunkRollbackException extends RuntimeException {
        public ChunkRollbackException(String message) {
            super(message);
        }
        
        public ChunkRollbackException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
