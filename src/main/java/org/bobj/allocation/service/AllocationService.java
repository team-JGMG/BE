package org.bobj.allocation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.allocation.domain.AllocationVO;
import org.bobj.allocation.dto.AllocationResponseDTO;
import org.bobj.allocation.dto.DividendPaymentDTO;
import org.bobj.allocation.mapper.AllocationMapper;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.notification.service.NotificationService;
import org.bobj.point.service.PointService;
import org.bobj.share.mapper.ShareMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationService {

    private final AllocationMapper allocationMapper;
    private final ShareMapper shareMapper;
    private final FundingMapper fundingMapper;

    private final PointService pointService;
    private final NotificationService notificationService;

    /**
     * 특정 펀딩의 배당금 내역 조회
     */
    public List<AllocationResponseDTO> getAllocationsByFundingId(Long fundingId) {
        log.info("펀딩 ID {}의 배당금 내역 조회 시작", fundingId);
        
        List<AllocationResponseDTO> allocations = allocationMapper.findAllocationsByFundingId(fundingId);
        
        log.info("펀딩 ID {}의 배당금 내역 조회 완료 - 총 {}건", fundingId, allocations.size());
        return allocations;
    }

    /**
     * 펀딩 완료 시 첫 배당금 생성 (한달 후 지급 예정)
     * 배당금 계산: 총 배당금 = 임대수익 * 90%, 주당 배당금 = 총 배당금 / 총 주식 수
     */
    @Transactional
    public void createFirstAllocation(Long fundingId) {
        log.info("펀딩 ID {}의 첫 배당금 생성 시작", fundingId);

        try {
            // 1. 임대수익과 총 주식 수 조회
            AllocationVO fundingInfo = allocationMapper.findRentalIncomeAndTotalShares(fundingId);
            if (fundingInfo == null) {
                log.error("펀딩 ID {}의 정보를 찾을 수 없습니다", fundingId);
                return;
            }

            // 2. 배당금 계산 (임대수익의 90% = 10% 수수료 제외)
            BigDecimal rentalIncome = fundingInfo.getDividendPerShare(); // XML에서 임시로 사용
            Integer totalShares = fundingInfo.getTotalDividendAmount().intValue(); // XML에서 임시로 사용
            
            if (rentalIncome == null || totalShares == null || totalShares == 0) {
                log.error("펀딩 ID {}의 임대수익 또는 총 주식 수가 유효하지 않습니다. 임대수익: {}, 총 주식 수: {}", 
                         fundingId, rentalIncome, totalShares);
                return;
            }

            // 총 배당금 = 임대수익의 90% (10% 수수료 제외)
            BigDecimal totalDividendAmount = rentalIncome.multiply(BigDecimal.valueOf(0.9))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            
            // 주당 배당금 = 총 배당금 / 총 주식 수
            BigDecimal dividendPerShare = totalDividendAmount.divide(BigDecimal.valueOf(totalShares), 2, BigDecimal.ROUND_HALF_UP);

            // 3. 첫 배당 지급일 설정 (오늘로부터 한달 후)
            LocalDate firstPaymentDate = LocalDate.now().plusMonths(1);

            // 4. 배당금 생성
            AllocationVO allocation = AllocationVO.builder()
                    .fundingId(fundingId)
                    .dividendPerShare(dividendPerShare)
                    .totalDividendAmount(totalDividendAmount)
                    .paymentDate(firstPaymentDate)
                    .paymentStatus("PENDING")
                    .build();

            allocationMapper.insertAllocation(allocation);

            log.info("펀딩 ID {}의 첫 배당금 생성 완료 - 임대수익: {}원, 총 배당금(90%): {}원, 주당 배당금: {}원, 지급일: {}", 
                    fundingId, rentalIncome, totalDividendAmount, dividendPerShare, firstPaymentDate);

        } catch (Exception e) {
            log.error("펀딩 ID {}의 첫 배당금 생성 실패", fundingId, e);
            throw new RuntimeException("배당금 생성 실패", e);
        }
    }

    /**
     * 배당 지급 처리 및 다음 배당 생성
     */
    @Transactional
    public void processPaymentAndCreateNext(LocalDate paymentDate) {
        log.info("배당 지급 처리 시작 - 지급일: {}", paymentDate);

        try {
            // 1. 지급일이 도래한 PENDING 상태의 배당금 조회
            List<AllocationVO> pendingAllocations = allocationMapper.findPendingAllocationsForPayment(paymentDate);
            
            log.info("지급 대상 배당금 {}건 발견", pendingAllocations.size());

            for (AllocationVO allocation : pendingAllocations) {
                try {
                    // 2. 배당금 지급 처리 (실제 포인트 지급 로직은 별도 구현 필요)
                    processAllocationPayment(allocation);

                    // 3. 상태를 COMPLETED로 변경
                    allocationMapper.updateAllocationStatus(allocation.getAllocationsId(), "COMPLETED");

                    // 4. 다음 배당금 생성 (한달 후)
                    createNextAllocation(allocation.getFundingId(), paymentDate);

                    log.info("펀딩 ID {}의 배당금 지급 및 다음 배당 생성 완료", allocation.getFundingId());

                } catch (Exception e) {
                    log.error("펀딩 ID {}의 배당금 처리 실패", allocation.getFundingId(), e);
                    // 개별 배당금 처리 실패 시 FAILED 상태로 변경
                    allocationMapper.updateAllocationStatus(allocation.getAllocationsId(), "FAILED");
                }
            }

            log.info("배당 지급 처리 완료 - 총 {}건 처리", pendingAllocations.size());

        } catch (Exception e) {
            log.error("배당 지급 처리 실패 - 지급일: {}", paymentDate, e);
            throw new RuntimeException("배당 지급 처리 실패", e);
        }
    }

    /**
     * 다음 배당금 생성 (한달 후 지급 예정)
     */
    private void createNextAllocation(Long fundingId, LocalDate currentPaymentDate) {
        log.info("펀딩 ID {}의 다음 배당금 생성 시작", fundingId);

        try {
            // 1. 임대수익과 총 주식 수 조회
            AllocationVO fundingInfo = allocationMapper.findRentalIncomeAndTotalShares(fundingId);
            if (fundingInfo == null) {
                log.error("펀딩 ID {}의 정보를 찾을 수 없습니다", fundingId);
                return;
            }

            // 2. 배당금 계산 (임대수익의 90% = 10% 수수료 제외)
            BigDecimal rentalIncome = fundingInfo.getDividendPerShare(); // XML에서 임시로 사용
            Integer totalShares = fundingInfo.getTotalDividendAmount().intValue(); // XML에서 임시로 사용

            // 총 배당금 = 임대수익의 90% (10% 수수료 제외)
            BigDecimal totalDividendAmount = rentalIncome.multiply(BigDecimal.valueOf(0.9))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            
            // 주당 배당금 = 총 배당금 / 총 주식 수
            BigDecimal dividendPerShare = totalDividendAmount.divide(BigDecimal.valueOf(totalShares), 2, BigDecimal.ROUND_HALF_UP);

            // 3. 다음 배당 지급일 설정 (현재 지급일로부터 한달 후)
            LocalDate nextPaymentDate = currentPaymentDate.plusMonths(1);

            // 4. 다음 배당금 생성
            AllocationVO nextAllocation = AllocationVO.builder()
                    .fundingId(fundingId)
                    .dividendPerShare(dividendPerShare)
                    .totalDividendAmount(totalDividendAmount)
                    .paymentDate(nextPaymentDate)
                    .paymentStatus("PENDING")
                    .build();

            allocationMapper.insertAllocation(nextAllocation);

            log.info("펀딩 ID {}의 다음 배당금 생성 완료 - 지급일: {}", fundingId, nextPaymentDate);

        } catch (Exception e) {
            log.error("펀딩 ID {}의 다음 배당금 생성 실패", fundingId, e);
        }
    }

    /**
     * 배당금 지급 처리 (실제 포인트 지급)
     */
    private void processAllocationPayment(AllocationVO allocation) {
        log.info("배당금 지급 처리 시작 - 배당금 ID: {}, 펀딩 ID: {}, 주당 배당금: {}원",
                allocation.getAllocationsId(), allocation.getFundingId(), allocation.getDividendPerShare());

        try {
            // 1. 해당 펀딩의 주식 보유자들 조회
            List<DividendPaymentDTO> shareholders = shareMapper.findShareHoldersByFundingId(allocation.getFundingId());
            
            if (shareholders.isEmpty()) {
                log.warn("배당 지급 대상자가 없습니다 - 펀딩 ID: {}", allocation.getFundingId());
                return;
            }

            log.info("배당 지급 대상자 {}명 조회 완료 - 펀딩 ID: {}", shareholders.size(), allocation.getFundingId());

            BigDecimal totalPaidAmount = BigDecimal.ZERO;
            int successCount = 0;
            int failCount = 0;

            // 배당금 지급 성공한 사용자 ID를 모아둘 리스트
            List<Long> userIdsForNotification = new ArrayList<>();

            // 2. 각 주식 보유자에게 배당금 지급
            for (DividendPaymentDTO shareholder : shareholders) {
                try {
                    // 개인별 배당금 계산
                    shareholder.setDividendPerShare(allocation.getDividendPerShare());
                    BigDecimal individualDividend = shareholder.calculateTotalDividend();

                    // 배당금 지급 (기존 PointService 재사용)
                    pointService.allocateDividend(
                        shareholder.getUserId(), 
                        individualDividend, 
                        allocation.getAllocationsId()
                    );
                    
                    totalPaidAmount = totalPaidAmount.add(individualDividend);
                    successCount++;

                    // 지급 성공한 사용자 ID를 알림 목록에 추가
                    userIdsForNotification.add(shareholder.getUserId());

                    log.info("✅ 배당금 지급 성공 - 사용자: {} (ID: {}), 보유주식: {}주, 배당금: {}원", 
                            shareholder.getUserName(), shareholder.getUserId(), 
                            shareholder.getShareCount(), individualDividend);

                } catch (Exception e) {
                    failCount++;
                    log.error("❌ 배당금 지급 실패 - 사용자 ID: {}, 보유주식: {}주", 
                             shareholder.getUserId(), shareholder.getShareCount(), e);
                }
            }

            log.info("🎯 배당금 지급 처리 완료 - 배당금 ID: {}, 성공: {}명, 실패: {}명, 총 지급액: {}원", 
                    allocation.getAllocationsId(), successCount, failCount, totalPaidAmount);


            // 3. 배당금 알림 전송
            sendAllocationNotifications(allocation.getFundingId(), userIdsForNotification);

            // 4. 실제 지급액과 예상 지급액 비교 검증
            if (totalPaidAmount.compareTo(allocation.getTotalDividendAmount()) != 0) {
                log.warn("⚠️ 배당금 지급액 불일치 - 예상: {}원, 실제: {}원", 
                        allocation.getTotalDividendAmount(), totalPaidAmount);
            }

        } catch (Exception e) {
            log.error("❌ 배당금 지급 처리 중 오류 발생 - 배당금 ID: {}", allocation.getAllocationsId(), e);
            throw new RuntimeException("배당금 지급 처리 실패", e);
        }
    }

   // 배당금 지급 완료 알림
    private void sendAllocationNotifications(Long fundingId, List<Long> userIdsForNotification) {
        if (userIdsForNotification.isEmpty()) {
            return;
        }

        try {
            String propertyTitle = fundingMapper.getPropertyTitleByFundingId(fundingId);

            String title = "배당금 지급 완료!";
            String body = "'" + propertyTitle + "' 배당금이 지급되었습니다. 포인트 내역을 확인해 주세요.";

            // 배치 전송 + 일괄 DB 저장
            notificationService.sendBatchNotificationsAndSave(userIdsForNotification, title, body);

            log.info("🔔 총 {}명의 사용자에게 배당금 알림을 배치 전송했습니다. (펀딩 ID: {})",
                    userIdsForNotification.size(), fundingId);

        } catch (Exception e) {
            log.error("참여자 대상 배당금 알림 전송 실패 - fundingId: {}", fundingId, e);
        }
    }
}
