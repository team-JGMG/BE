package org.bobj.funding.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.allocation.service.AllocationService;
import org.bobj.common.dto.CustomSlice;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.domain.FundingVO;
import org.bobj.funding.dto.FundingOrderLimitDTO;
import org.bobj.funding.dto.FundingOrderUserResponseDTO;
import org.bobj.funding.event.ShareDistributionEvent;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.funding.mapper.FundingOrderMapper;
import org.bobj.notification.service.NotificationService;
import org.bobj.point.service.PointService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class FundingOrderService {
    private final FundingOrderMapper fundingOrderMapper;
    private final FundingMapper fundingMapper;

    private final ApplicationEventPublisher eventPublisher;
    private final AllocationService allocationService;
    private final PointService pointService;

    private final NotificationService notificationService;

    // 주문 추가
    @Transactional
    public void createFundingOrder(Long userId, Long fundingId, int shareCount) {
        FundingVO funding = fundingMapper.findByIdWithLock(fundingId);
        if (funding == null) {
            throw new IllegalArgumentException("존재하지 않는 펀딩입니다.");
        }

        int remainingShares = funding.getRemainingShares();
        if (shareCount > remainingShares) {
            throw new IllegalArgumentException("남은 주 수를 초과했습니다.");
        }

        BigDecimal sharePrice = BigDecimal.valueOf(5000);
        BigDecimal orderPrice = sharePrice.multiply(BigDecimal.valueOf(shareCount));

        /* 요기 구현해주시면 됩니다! (Point) */

        // user의 point 가져오는 api 부탁드려요!! PointMapper.findUserPoints(userId)
        BigDecimal userPoints = pointService.getTotalPoint(userId);

//        BigDecimal userPoints = BigDecimal.valueOf(100000);
        if (userPoints.compareTo(orderPrice) < 0) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

        // 주문 생성
        fundingOrderMapper.insertFundingOrder(userId, fundingId, shareCount, orderPrice);

        /* 요기 구현해주시면 됩니다! (Point) */
        // 포인트 차감 과정(추후 구현)
        pointService.investPoint(userId, orderPrice);

        // 펀딩 현재 모인 금액 증가
        fundingMapper.increaseCurrentAmount(fundingId, orderPrice);

        // 펀딩 완료 상태 체크
        BigDecimal updatedAmount = funding.getCurrentAmount().add(orderPrice);
        if (updatedAmount.compareTo(funding.getTargetAmount()) >= 0) {
            log.info(" 펀딩 목표 달성! 펀딩 완료 처리 시작 - 펀딩 ID: {}", fundingId);

            // 1. 펀딩을 완료 상태로 변경
            fundingMapper.markAsEnded(fundingId);

            // 2. 모든 주문을 성공 상태로 변경
            fundingOrderMapper.markOrdersAsSuccessByFundingId(fundingId);

            // 펀딩 성공 알림 전송 메서드 호출
            sendFundingSuccessNotifications(fundingId);
            // 3. 지분 분배 이벤트 발행
            eventPublisher.publishEvent(new ShareDistributionEvent(fundingId));

            // 4. 첫 배당금 생성 (한달 후 지급 예정)
            try {
                allocationService.createFirstAllocation(fundingId);
                log.info("펀딩 완료 및 첫 배당금 생성 성공 - 펀딩 ID: {}", fundingId);
            } catch (Exception e) {
                log.error("첫 배당금 생성 실패 - 펀딩 ID: {} (펀딩 완료는 정상 처리됨)", fundingId, e);
                // 배당금 생성 실패가 펀딩 완료를 방해하지 않도록 예외를 전파하지 않음
            }
        }
    }

    // 주문 취소
    @Transactional
    public void refundFundingOrder(Long orderId, Long fundingId, BigDecimal orderPrice) {
        // 1. 주문 상태 REFUNDED 처리
        fundingOrderMapper.refundFundingOrder(orderId);

        // 2. 펀딩 누적 금액 차감
        fundingMapper.decreaseCurrentAmount(fundingId, orderPrice);

        // 3. 사용자 정보 조회
        FundingOrderVO order = fundingOrderMapper.findById(orderId); // userId 포함되어 있어야 함
        Long userId = order.getUserId();

        // 4. 포인트 환불 처리
        pointService.refundForFundingCancel(userId, orderPrice);
    }

    // 내가 투자한 주문 리스트
    public CustomSlice<FundingOrderUserResponseDTO> getFundingOrderUsers(Long userId, String status, int page, int size) {
        int offset = page*size;
        String upperStatus = status.toUpperCase();

        List<FundingOrderUserResponseDTO> content = fundingOrderMapper.findFundingOrdersByUserId(userId,upperStatus,offset,size+1);
        boolean hasNext = content.size() > size;
        if (hasNext) {
            content.remove(size);
        }

        return new CustomSlice<>(content,hasNext);
    }
    // 주문 가능 정보 조회
    @Transactional(readOnly = true)
    public FundingOrderLimitDTO getFundingOrderLimit(Long userId, Long fundingId) {
        return fundingOrderMapper.findFundingOrderLimit(userId, fundingId);
    }

    private void sendFundingSuccessNotifications(Long fundingId) {
        String propertyTitle = fundingMapper.getPropertyTitleByFundingId(fundingId);

        // 1. 등록자 알림 (펀딩을 올린 사용자)
        Long ownerUserId = fundingMapper.getUserIdbyFundingId(fundingId);

        if (ownerUserId != null) {
            String title = "펀딩 성공!";
            String body = "'" + propertyTitle + "' 펀딩이 목표 금액 달성에 성공했습니다. 축하합니다!";
            notificationService.sendNotificationAndSave(ownerUserId, title, body);
        }

        // 2. 참여자 알림
        // 펀딩 ID로 모든 참여자들의 userId를 조회합니다.
        List<Long> participantUserIds = fundingOrderMapper.findAllOrdersByFundingId(fundingId).stream()
                .map(FundingOrderVO::getUserId)
                .distinct()
                .toList();

        if (!participantUserIds.isEmpty()) {
            String title = "펀딩 성공!";
            String body = "'" + propertyTitle + "' 펀딩이 목표 금액 달성에 성공했습니다. 지분이 곧 분배됩니다.";
            for (Long participantId : participantUserIds) {
                notificationService.sendNotificationAndSave(participantId, title, body);
            }
        }
    }
}
